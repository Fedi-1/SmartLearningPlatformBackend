package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.enums.DocumentStatus;
import com.example.SmartLearningPlatformBackend.exception.DuplicateDocumentException;
import com.example.SmartLearningPlatformBackend.exception.UnsupportedFileTypeException;
import com.example.SmartLearningPlatformBackend.models.Student;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private CourseService courseService;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizAnswerRepository quizAnswerRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private ExamAttemptQuestionRepository examAttemptQuestionRepository;
    @Mock
    private ExamAnswerRepository examAnswerRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private SuspiciousActivityRepository suspiciousActivityRepository;
    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private MultipartFile mockFile;
    @Mock
    private Student mockStudent;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void uploadAndGenerate_throwsWhenFileEmpty() {
        when(mockFile.isEmpty()).thenReturn(true);
        when(mockFile.getOriginalFilename()).thenReturn("empty.pdf");

        assertThrows(IllegalArgumentException.class,
                () -> documentService.uploadAndGenerate(mockFile, mockStudent));
    }

    @Test
    void uploadAndGenerate_throwsWhenUnsupportedFileType() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("malware.exe");

        assertThrows(UnsupportedFileTypeException.class,
                () -> documentService.uploadAndGenerate(mockFile, mockStudent));
    }

    @Test
    void uploadAndGenerate_throwsWhenDuplicateHash() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("lecture.pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getBytes()).thenReturn("dummy content".getBytes());
        when(mockStudent.getId()).thenReturn(1L);

        when(documentRepository.existsByStudentIdAndFileHashAndStatusNot(
                eq(1L), anyString(), eq(DocumentStatus.FAILED)))
                .thenReturn(true);

        assertThrows(DuplicateDocumentException.class,
                () -> documentService.uploadAndGenerate(mockFile, mockStudent));
    }
}
