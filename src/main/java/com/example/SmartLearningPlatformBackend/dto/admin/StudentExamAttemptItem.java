// src/main/java/.../dto/admin/StudentExamAttemptItem.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamAttemptItem {
    private Long id;
    private String courseTitle;
    private Integer score;
    private Boolean isPassed;
    private Integer attemptNumber;
    private String submittedAt;
}
