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
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${app.frontend.url}")
  private String frontendUrl;

  @org.springframework.beans.factory.annotation.Value("${frontend.base-url:http://localhost:4200}")
  private String frontendBaseUrl;

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
    sendEmailNotification(userId, title, message, actionUrl, null);
  }

  @Async
  public void sendEmailNotification(Long userId, String title, String message, String actionUrl,
      NotificationCategory category) {
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

      String buttonLabel = (category == NotificationCategory.CERTIFICATE)
          ? "Download Certificate"
          : "View Details";
      helper.setText(buildEmailHtml(title, message, actionUrl, user.getFirstName(), buttonLabel), true);

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

    // Append base URL cleanly whether relative or absolute
    String emailActionUrl = actionUrl;
    if (actionUrl != null && !actionUrl.startsWith("http")) {
      emailActionUrl = frontendBaseUrl + actionUrl;
    }

    sendEmailNotification(userId, title, message, emailActionUrl, category);
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

  private String buildEmailHtml(String title, String message, String actionUrl, String firstName,
      String buttonLabel) {
    String buttonHtml = "";
    String titleHtml = "";
    String extraContentHtml = "";

    if (title.contains("Verify your LearnAI account")) {
      titleHtml = "Welcome to LearnAI!";
      extraContentHtml = """
            <p style="margin: 0 0 16px; color: #94a3b8; font-size: 14px; text-align: center;">
              <span style="color: #6366f1;">⚠️</span> This link will expire in 24 hours.
            </p>
            <p style="margin: 0; color: #94a3b8; font-size: 14px; text-align: center;">
              If you did not sign up for LearnAI, you can safely ignore this email.
            </p>
          """;
      buttonLabel = "Verify My Account";
    } else if (title.contains("Reset your LearnAI password")) {
      titleHtml = "Password Reset Request";
      extraContentHtml = """
            <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color: rgba(99, 102, 241, 0.1); border: 1px solid rgba(99, 102, 241, 0.2); border-radius: 8px;">
              <tr>
                <td style="padding: 16px; text-align: center;">
                  <p style="margin: 0 0 8px; color: #e2e8f0; font-size: 14px; font-weight: 600;">
                    Security Warning
                  </p>
                  <p style="margin: 0; color: #94a3b8; font-size: 13px; line-height: 1.5;">
                    This link will expire in <strong>15 minutes</strong>. If you did not request a password reset, please safely ignore this email. Your account remains secure.
                  </p>
                </td>
              </tr>
            </table>
          """;
      buttonLabel = "Reset My Password";
    } else if (title.contains("Your certificate is ready!")) {
      titleHtml = "<div style=\"text-align: center; margin-bottom: 20px;\"><span style=\"font-size: 48px;\">🎓</span></div><h2 style=\"margin: 0 0 20px; color: #ffffff; font-size: 22px; font-weight: 600; text-align: center;\">Congratulations!</h2>";
      extraContentHtml = """
            <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color: #0f1117; border-radius: 8px;">
              <tr>
                <td style="padding: 16px; text-align: center;">
                  <p style="margin: 0 0 4px; color: #94a3b8; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;">Verification Notice</p>
                  <p style="margin: 0; color: #6366f1; font-size: 14px;">Your certificate has been securely verified.</p>
                </td>
              </tr>
            </table>
          """;
    } else if (title.contains("certificate has been revoked")) {
      titleHtml = "Certificate Status Update";
      extraContentHtml = """
           <table width="100%" cellpadding="0" cellspacing="0" border="0">
             <tr>
               <td align="center">
                 <a href="mailto:support@learnai.com" style="display: inline-block; padding: 14px 28px; background-color: transparent; color: #ffffff; border: 1px solid #6366f1; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">Contact Administration</a>
               </td>
             </tr>
           </table>
          """;
      actionUrl = null;
    } else if (title.contains("Don't lose your streak")) {
      titleHtml = "<div style=\"text-align: center; margin-bottom: 20px;\"><span style=\"font-size: 40px;\">🔥</span></div><h2 style=\"margin: 0 0 20px; color: #ffffff; font-size: 22px; font-weight: 600; text-align: center;\">Don't lose your momentum!</h2>";
      buttonLabel = "Resume Learning";
      extraContentHtml = """
           <p style="margin: 0; color: #94a3b8; font-size: 14px; text-align: center;">
             Jump back in for just 10 minutes today to keep your streak alive!
           </p>
          """;
    } else {
      titleHtml = title;
    }

    if (actionUrl != null && !actionUrl.isBlank() && !title.contains("certificate has been revoked")) {
      buttonHtml = """
          <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin-bottom: 24px;">
            <tr>
              <td align="center">
                <a href="%s" style="display: inline-block; padding: 14px 32px; background-color: #6366f1; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px; box-shadow: 0 4px 14px rgba(99, 102, 241, 0.2);">%s</a>
              </td>
            </tr>
          </table>
          """
          .formatted(actionUrl, buttonLabel);
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>%s</title>
        </head>
        <body style="margin: 0; padding: 0; background-color: #0f1117; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #e2e8f0; -webkit-font-smoothing: antialiased;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #0f1117; width: 100%%; margin: 0 auto;">
            <tr>
              <td align="center" style="padding: 40px 20px;">
                <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width: 600px; width: 100%%; background-color: #1a1d27; border-collapse: separate; border-radius: 12px; border: 1px solid #2d313f; box-shadow: 0 0 20px rgba(99, 102, 241, 0.1); overflow: hidden;">

                  <tr>
                    <td align="center" style="background-color: #4f46e5; background: linear-gradient(135deg, #4338ca, #6366f1); padding: 24px;">
                      <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: 1px;">✦ LearnAI</h1>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding: 40px 32px;">
                      <p style="margin: 0 0 16px; color: #94a3b8; font-size: 14px;">Hi %s,</p>
                      %s
                      <p style="margin: 0 0 24px; color: #cbd5e1; font-size: 16px; line-height: 1.6;">
                        %s
                      </p>

                      %s

                      %s
                    </td>
                  </tr>

                  <tr>
                    <td align="center" style="padding: 24px 32px; background-color: #1a1d27; border-top: 1px solid #2d313f;">
                      <p style="margin: 0; color: #64748b; font-size: 13px;">
                        © 2026 LearnAI — Smart Learning Platform
                      </p>
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """
        .formatted(title, firstName != null ? firstName : "Student",
            (titleHtml.startsWith("<") ? titleHtml
                : "<h2 style=\"margin: 0 0 20px; color: #ffffff; font-size: 22px; font-weight: 600;\">"
                    + titleHtml + "</h2>"),
            message, buttonHtml, extraContentHtml);
  }
}
