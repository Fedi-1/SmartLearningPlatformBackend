package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.ai.AiCourseResponse;
import com.example.SmartLearningPlatformBackend.dto.document.DocumentResponse;
import com.example.SmartLearningPlatformBackend.dto.document.UploadResponse;
import com.example.SmartLearningPlatformBackend.enums.DocumentStatus;
import com.example.SmartLearningPlatformBackend.enums.FileType;
import com.example.SmartLearningPlatformBackend.exception.FileTooLargeException;
import com.example.SmartLearningPlatformBackend.exception.UnsupportedFileTypeException;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Document;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "docx", "pptx", "jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private final DocumentRepository documentRepository;
    private final CourseRepository courseRepository;
    private final AiServiceClient aiServiceClient;
    private final CourseService courseService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // ─── Upload & Generate ──────────────────────────────────────────────────────

    public UploadResponse uploadAndGenerate(MultipartFile file, Student student) {

        // 1. Validate
        String originalFilename = file.getOriginalFilename();
        if (file.isEmpty() || originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Uploaded file is empty or has no name.");
        }
        if (!isAllowedExtension(originalFilename)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type. Allowed: pdf, docx, pptx, jpg, jpeg, png, webp.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException("File exceeds the 10 MB size limit.");
        }

        FileType fileType = determineFileType(originalFilename);

        // 2. Save file to disk
        String storedName = UUID.randomUUID() + "_" + originalFilename;
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(storedName));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file to disk: " + e.getMessage());
        }

        // 3. Persist Document with PROCESSING status
        Document document = Document.builder()
                .studentId(student.getId())
                .fileName(originalFilename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .filePath(uploadPath.resolve(storedName).toString())
                .status(DocumentStatus.PROCESSING)
                .build();
        document = documentRepository.save(document);

        // 4. Call AI service
        AiCourseResponse aiResponse;
        try {
            aiResponse = aiServiceClient.processDocument(file, fileType);
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw e;
        }

        // 5. Persist course, lessons, quizzes, flashcards
        Course course;
        try {
            course = courseService.generateAndSave(aiResponse, document, student);
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new RuntimeException("Failed to save generated course: " + e.getMessage());
        }

        // 6. Mark document COMPLETED and set detected category
        document.setStatus(DocumentStatus.COMPLETED);
        document.setCategory(aiResponse.getCategory());
        documentRepository.save(document);

        return UploadResponse.builder()
                .documentId(document.getId())
                .courseId(course.getId())
                .courseTitle(aiResponse.getCourseTitle())
                .totalLessons(aiResponse.getTotalLessons())
                .build();
    }

    // ─── List documents ─────────────────────────────────────────────────────────

    public List<DocumentResponse> getStudentDocuments(Long studentId) {
        return documentRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .map(doc -> {
                    Optional<Course> course = courseRepository.findByDocumentId(doc.getId());
                    return DocumentResponse.builder()
                            .id(doc.getId())
                            .fileName(doc.getFileName())
                            .fileType(doc.getFileType())
                            .fileSize(doc.getFileSize())
                            .uploadedAt(doc.getUploadedAt())
                            .status(doc.getStatus())
                            .courseId(course.map(Course::getId).orElse(null))
                            .category(doc.getCategory())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Soft delete ────────────────────────────────────────────────────────────

    @Transactional
    public void softDeleteDocument(Long documentId, Long studentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));
        if (!document.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Access denied.");
        }
        document.setIsDeleted(true);
        documentRepository.save(document);

        // Propagate deletion to the course generated from this document
        courseRepository.findByDocumentId(documentId).ifPresent(course -> {
            course.setIsDeleted(true);
            courseRepository.save(course);
        });
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    public FileType determineFileType(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid filename: " + originalFilename);
        }
        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase();
        return switch (extension) {
            case "pdf" -> FileType.PDF;
            case "docx" -> FileType.DOCX;
            case "pptx" -> FileType.PPTX;
            case "jpg", "jpeg", "png", "webp" -> FileType.IMAGE;
            default -> throw new UnsupportedFileTypeException("Unsupported file type: " + extension);
        };
    }

    public boolean isAllowedExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains("."))
            return false;
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }
}
