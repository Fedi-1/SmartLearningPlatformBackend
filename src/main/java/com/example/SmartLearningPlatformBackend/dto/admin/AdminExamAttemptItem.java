// src/main/java/com/example/SmartLearningPlatformBackend/dto/admin/AdminExamAttemptItem.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminExamAttemptItem {
    private Long attemptId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String courseTitle;
    private String category;
    private Integer score;
    private Boolean isPassed;
    private Integer attemptNumber;
    private String submittedAt;
    private Boolean hasSuspiciousActivity;
    private Integer suspiciousEventsCount;
}
