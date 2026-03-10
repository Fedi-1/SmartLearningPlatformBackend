// src/main/java/.../dto/admin/StudentSummaryResponse.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSummaryResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean isActive;
    private String createdAt;
    private String lastLogin;
    private int coursesCount;
    private int engagementScore;
    private String engagementLevel;
}
