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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

        // 2. Read file bytes and compute SHA-256 hash
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes: " + e.getMessage());
        }
        String fileHash = HashingUtil.computeSHA256(fileBytes);

        // 3. Save file to disk
        String storedName = UUID.randomUUID() + "_" + originalFilename;
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Files.write(uploadPath.resolve(storedName), fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file to disk: " + e.getMessage());
        }

        // 4. Persist Document with fileHash and PROCESSING status
        Document document = Document.builder()
                .studentId(student.getId())
                .fileName(originalFilename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .filePath(uploadPath.resolve(storedName).toString())
                .fileHash(fileHash)
                .status(DocumentStatus.PROCESSING)
                .build();
        final Document savedDocument = documentRepository.save(document);

        // 5. Check for existing document with same hash (excluding current document)
        Optional<Document> existingDocOpt = documentRepository.findFirstByFileHash(fileHash)
                .filter(existing -> !existing.getId().equals(savedDocument.getId()));

        Course course;

        if (existingDocOpt.isPresent()) {
            Document existingDocument = existingDocOpt.get();
            log.info("Hash match found for document {} — cloning course from document {}",
                    savedDocument.getId(), existingDocument.getId());

            Optional<Course> existingCourseOpt = courseRepository.findByDocumentId(existingDocument.getId());

            if (existingCourseOpt.isPresent()) {
                // Clone the existing course for this student
                course = courseService.cloneCourseForStudent(existingCourseOpt.get(), savedDocument, student.getId());

                savedDocument.setStatus(DocumentStatus.COMPLETED);
                savedDocument.setCategory(existingCourseOpt.get().getCategory());
                documentRepository.save(savedDocument);

                return UploadResponse.builder()
                        .documentId(savedDocument.getId())
                        .courseId(course.getId())
                        .courseTitle(course.getTitle())
                        .totalLessons(null)
                        .build();
            }
            // Existing document has no course — fall through to normal AI generation
        } else {
            log.info("No hash match for document {} — proceeding with AI generation", savedDocument.getId());
        }

        // 6. Call AI service
        AiCourseResponse aiResponse;
        try {
            aiResponse = aiServiceClient.processDocument(file, fileType);
        } catch (Exception e) {
            savedDocument.setStatus(DocumentStatus.FAILED);
            documentRepository.save(savedDocument);
            throw e;
        }

        // 7. Persist course, lessons, quizzes, flashcards
        try {
            course = courseService.generateAndSave(aiResponse, savedDocument, student);
        } catch (Exception e) {
            savedDocument.setStatus(DocumentStatus.FAILED);
            documentRepository.save(savedDocument);
            throw new RuntimeException("Failed to save generated course: " + e.getMessage());
        }

        // 8. Mark document COMPLETED and set detected category
        savedDocument.setStatus(DocumentStatus.COMPLETED);
        savedDocument.setCategory(aiResponse.getCategory());
        documentRepository.save(savedDocument);

        return UploadResponse.builder()
                .documentId(savedDocument.getId())
                .courseId(course.getId())
                .courseTitle(aiResponse.getCourseTitle())
                .totalLessons(aiResponse.getTotalLessons())
                .build();
    }

    // ─── List documents ─────────────────────────────────────────────────────────

    public List<DocumentResponse> getStudentDocuments(Long studentId) {
        return documentRepository.findByStudentId(studentId)
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

    // ─── Delete ─────────────────────────────────────────────────────────────────

    @Transactional
    public void softDeleteDocument(Long documentId, Long studentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));
        if (!document.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Access denied.");
        }
        documentRepository.delete(document);
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
