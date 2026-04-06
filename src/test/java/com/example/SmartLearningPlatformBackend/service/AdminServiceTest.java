package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.certificate.CertificateDTO;
import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.repository.ActivityLogRepository;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.DocumentRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamRepository;
import com.example.SmartLearningPlatformBackend.repository.FlashcardReviewRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonProgressRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.SuspiciousActivityRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private FlashcardReviewRepository flashcardReviewRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SuspiciousActivityRepository suspiciousActivityRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "backendBaseUrl", "http://localhost:8069");
        ReflectionTestUtils.setField(adminService, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    void approveCertificate_setsStatusToApproved() {
        Student student = new Student();
        student.setId(1L);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john@example.com");

        Course course = new Course();
        course.setId(1L);
        course.setTitle("A Test Course");

        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setStatus(CertificateStatus.PENDING);
        certificate.setCertificateUuid("test-uuid-123");
        certificate.setStudent(student);
        certificate.setCourse(course);

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(certificate));
        when(certificateService.generatePdf(any(Long.class), any()))
                .thenReturn(CertificateDTO.builder().build());
        adminService.approveCertificate(1L);

        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(captor.capture());

        Certificate savedCert = captor.getValue();
        assertEquals(CertificateStatus.APPROVED, savedCert.getStatus());
    }

    @Test
    void revokeCertificate_setsStatusToRevoked() {
        Student student = new Student();
        student.setId(2L);
        student.setFirstName("Jane");
        student.setLastName("Doe");
        student.setEmail("jane@example.com");

        Course course = new Course();
        course.setId(2L);
        course.setTitle("Another Test Course");

        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setStatus(CertificateStatus.APPROVED);
        certificate.setStudent(student);
        certificate.setCourse(course);

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(certificate));

        adminService.revokeCertificate(1L);

        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(captor.capture());

        Certificate savedCert = captor.getValue();
        assertEquals(CertificateStatus.REVOKED, savedCert.getStatus());
    }
}
