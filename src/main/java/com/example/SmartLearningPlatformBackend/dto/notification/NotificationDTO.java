package com.example.SmartLearningPlatformBackend.dto.notification;

import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private Long userId;
    private NotificationType type;
    private NotificationCategory category;
    private String title;
    private String message;
    private Long referenceId;
    private String actionUrl;
    private Boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
