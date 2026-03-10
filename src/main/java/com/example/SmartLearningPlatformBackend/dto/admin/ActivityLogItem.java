package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityLogItem {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String action;
    private String entityType;
    private Long entityId;
    private String timestamp;
}
