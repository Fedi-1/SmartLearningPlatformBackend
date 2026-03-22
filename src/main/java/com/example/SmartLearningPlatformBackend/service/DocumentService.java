package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.ai.AiCourseResponse;
import com.example.SmartLearningPlatformBackend.dto.document.DocumentResponse;
import com.example.SmartLearningPlatformBackend.dto.document.UploadResponse;
import com.example.SmartLearningPlatformBackend.enums.DocumentStatus;
import com.example.SmartLearningPlatformBackend.enums.FileType;
import com.example.SmartLearningPlatformBackend.exception.FileTooLargeException;
import com.example.SmartLearningPlatformBackend.exception.UnsupportedFileTypeException;
import com.example.SmartLearningPlatformBackend.enums.ActionType;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Document;
import com.example.SmartLearningPlatformBackend.models.ExamAttempt;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.exception.DuplicateDocumentException;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.DocumentRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAnswerRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptQuestionRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAnswerRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizRepository;
import com.example.SmartLearningPlatformBackend.repository.SuspiciousActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptQuestionRepository examAttemptQuestionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final CertificateRepository certificateRepository;
    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final ActivityLogService activityLogService;

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

        if (documentRepository.existsByStudentIdAndFileHashAndStatusNot(student.getId(), fileHash,
                DocumentStatus.FAILED)) {
            throw new DuplicateDocumentException("You have already uploaded this document.");
        }

        documentRepository.findByStudentIdAndFileHash(student.getId(), fileHash)
                .ifPresent(existing -> {
                    if (DocumentStatus.FAILED.equals(existing.getStatus())) {
                        documentRepository.delete(existing);
                    }
                });

        // 3. Persist Document with file bytes and PROCESSING status
        Document document = Document.builder()
                .studentId(student.getId())
                .fileName(originalFilename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .fileContent(fileBytes)
                .fileHash(fileHash)
                .status(DocumentStatus.PROCESSING)
                .build();
        final Document savedDocument = documentRepository.save(document);

        // Log the document upload
        activityLogService.log(student.getId(), ActionType.UPLOAD_DOCUMENT, "Document", savedDocument.getId());

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

                // Log the course generation activity
                activityLogService.log(student.getId(), ActionType.GENERATE_COURSE, "Course", course.getId());

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
            aiResponse = aiServiceClient.processDocument(savedDocument.getFileContent(), originalFilename, fileType);
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

        // Log the course generation activity
        activityLogService.log(student.getId(), ActionType.GENERATE_COURSE, "Course", course.getId());

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

        courseRepository.findByDocumentId(documentId).ifPresent(course -> {

            // ── 1. Delete exam attempt data ──────────────────────────────────────
            examRepository.findByCourseId(course.getId()).ifPresent(exam -> {
                List<ExamAttempt> attempts = examAttemptRepository.findByExamId(exam.getId());
                List<Long> attemptIds = attempts.stream()
                        .map(ExamAttempt::getId)
                        .collect(Collectors.toList());

                if (!attemptIds.isEmpty()) {
                    // Delete leaf rows that reference exam_attempts (strict FK order)
                    examAnswerRepository.deleteAllByExamAttemptIdIn(attemptIds);
                    examAttemptQuestionRepository.deleteAllByExamAttemptIdIn(attemptIds);
                    certificateRepository.deleteAllByExamAttemptIdIn(attemptIds);
                    suspiciousActivityRepository.deleteAllByExamAttemptIdIn(attemptIds);
                    examAttemptRepository.deleteAllById(attemptIds);
                }

                examRepository.deleteById(exam.getId());
            });

            // ── 2. Pre-delete quiz answer data before JPA cascade removes quiz_attempts ──
            List<Long> lessonIds = lessonRepository.findByCourseIdOrderByLessonNumberAsc(course.getId())
                    .stream().map(l -> l.getId()).collect(Collectors.toList());

            if (!lessonIds.isEmpty()) {
                List<Long> quizIds = quizRepository.findByLessonIdIn(lessonIds)
                        .stream().map(q -> q.getId()).collect(Collectors.toList());

                if (!quizIds.isEmpty()) {
                    List<Long> quizAttemptIds = quizAttemptRepository.findByQuizIdIn(quizIds)
                            .stream().map(qa -> qa.getId()).collect(Collectors.toList());

                    if (!quizAttemptIds.isEmpty()) {
                        quizAnswerRepository.deleteAllByQuizAttemptIdIn(quizAttemptIds);
                    }
                }
            }

            // ── 3. Delete the course (JPA cascade handles everything else) ───────
            // course → lessons → flashcards → flashcard_reviews
            // → quizzes → quiz_questions → quiz_answers (JPA)
            // → quiz_attempts → quiz_answers (JPA)
            // → lesson_progress
            courseRepository.delete(course);
        });

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
