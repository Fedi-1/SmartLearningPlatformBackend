package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private CertificateService certificateService;

    @Test
    void downloadPdf_throwsWhenPending() {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setStudentId(1L);
        certificate.setStatus(CertificateStatus.PENDING);

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(certificate));

        assertThrows(IllegalStateException.class, () -> certificateService.downloadPdf(1L, 1L));
    }

    @Test
    void downloadPdf_throwsWhenRevoked() {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setStudentId(1L);
        certificate.setStatus(CertificateStatus.REVOKED);

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(certificate));

        assertThrows(IllegalStateException.class, () -> certificateService.downloadPdf(1L, 1L));
    }
}
