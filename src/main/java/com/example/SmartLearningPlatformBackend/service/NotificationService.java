package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.notification.NotificationDTO;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.enums.NotificationType;
import com.example.SmartLearningPlatformBackend.models.Notification;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.repository.NotificationRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SseService sseService;

    /**
     * Master method — decides which channels to fire based on category.
     */
    public void notify(Long userId, NotificationCategory category, String title,
            String message, Long referenceId, String actionUrl) {
        if (userId == null || category == null || title == null || message == null) {
            throw new IllegalArgumentException("userId, category, title and message are required");
        }

        switch (category) {
            case EXAM_RESULT, CERTIFICATE -> {
                createAndPushInApp(userId, category, title, message, referenceId, actionUrl);
                createEmailRecord(userId, category, title, message, referenceId, actionUrl);
            }
            case COURSE_COMPLETE, SUSPICIOUS_ACTIVITY -> {
                createAndPushInApp(userId, category, title, message, referenceId, actionUrl);
            }
            case REMINDER -> {
                createEmailRecord(userId, category, title, message, referenceId, actionUrl);
            }
        }
    }

    public Notification createNotification(Long userId, NotificationType type, NotificationCategory category,
            String title, String message, Long referenceId, String actionUrl) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .category(category)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .actionUrl(actionUrl)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Async
    public void sendEmailNotification(Long userId, String title, String message, String actionUrl) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) {
            log.warn("Cannot send email: user {} not found or has no email", userId);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("noreply.learnai@gmail.com");
            helper.setTo(user.getEmail());
            helper.setSubject(title);
            helper.setText(buildEmailHtml(title, message, actionUrl, user.getFirstName()), true);

            mailSender.send(mimeMessage);
            log.info("Email sent to {} for: {}", user.getEmail(), title);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendInAppNotification(Long userId, Notification notification) {
        if (userId == null || notification == null) {
            return;
        }
        sseService.sendToUser(userId, toDTO(notification));
    }

    public NotificationDTO markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        return toDTO(notificationRepository.save(notification));
    }

    public List<NotificationDTO> getUserNotifications(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void createAndPushInApp(Long userId, NotificationCategory category,
            String title, String message, Long referenceId, String actionUrl) {
        Notification saved = createNotification(userId, NotificationType.IN_APP, category,
                title, message, referenceId, actionUrl);
        sendInAppNotification(userId, saved);
    }

    private void createEmailRecord(Long userId, NotificationCategory category,
            String title, String message, Long referenceId, String actionUrl) {
        // EMAIL notifications are considered already "read" — they are delivered
        // externally
        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .category(category)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .actionUrl(actionUrl)
                .isRead(true)
                .readAt(java.time.LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        sendEmailNotification(userId, title, message, actionUrl);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .category(n.getCategory())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .actionUrl(n.getActionUrl())
                .isRead(n.getIsRead())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .build();
    }

    private String buildEmailHtml(String title, String message, String actionUrl, String firstName) {
        String buttonHtml = "";
        if (actionUrl != null && !actionUrl.isBlank()) {
            buttonHtml = """
                    <div style="text-align:center;margin:28px 0;">
                      <a href="%s"
                         style="display:inline-block;padding:12px 32px;
                                background:#6366F1;color:#ffffff;
                                text-decoration:none;border-radius:8px;
                                font-weight:600;font-size:15px;">
                        View Details
                      </a>
                    </div>
                    """.formatted(actionUrl);
        }

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;font-family:'Segoe UI',Arial,sans-serif;background:#F1F5F9;">
                  <div style="max-width:560px;margin:32px auto;background:#ffffff;border-radius:12px;
                              overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                    <div style="background:linear-gradient(135deg,#6366F1,#4F46E5);padding:28px 32px;">
                      <h1 style="margin:0;color:#ffffff;font-size:22px;">LearnAI</h1>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 8px;color:#64748B;font-size:14px;">Hi %s,</p>
                      <h2 style="margin:0 0 16px;color:#0F172A;font-size:18px;">%s</h2>
                      <p style="margin:0 0 24px;color:#334155;font-size:15px;line-height:1.6;">%s</p>
                      %s
                    </div>
                    <div style="padding:16px 32px;background:#F8FAFC;text-align:center;">
                      <p style="margin:0;color:#94A3B8;font-size:12px;">
                        &copy; 2026 LearnAI &mdash; Smart Learning Platform
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(firstName != null ? firstName : "Student", title, message, buttonHtml);
    }
}
