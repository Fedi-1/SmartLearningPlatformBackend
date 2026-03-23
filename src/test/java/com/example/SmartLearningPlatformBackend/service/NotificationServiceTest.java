package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.models.Notification;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.repository.NotificationRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SseService sseService;

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private MimeMessage mimeMessage;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("john@example.com");
        user.setFirstName("John");
    }

    @Test
    void notify_savesInAppNotification_forCertificateCategory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.notify(
                1L,
                NotificationCategory.CERTIFICATE,
                "Certificate Approved",
                "Your certificate is ready",
                1L,
                "http://localhost:8069/api/certificates/uuid/download");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());

        // Validate the first notification saved (the IN_APP one)
        Notification savedNotification = captor.getAllValues().get(0);
        assertEquals(1L, savedNotification.getUserId());
        assertEquals(NotificationCategory.CERTIFICATE, savedNotification.getCategory());
        assertEquals("Certificate Approved", savedNotification.getTitle());
    }

    @Test
    void notify_sendsEmail_forCertificateCategory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.notify(
                1L,
                NotificationCategory.CERTIFICATE,
                "Certificate Approved",
                "Your certificate is ready",
                1L,
                "http://localhost:8069/api/certificates/uuid/download");

        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class));
    }
}
