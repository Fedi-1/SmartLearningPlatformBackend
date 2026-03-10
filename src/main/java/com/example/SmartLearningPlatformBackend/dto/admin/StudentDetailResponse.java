// src/main/java/.../dto/admin/StudentDetailResponse.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean isActive;
    private String createdAt;
    private String lastLogin;
    private int engagementScore;
    private String engagementLevel;
    private List<StudentCourseItem> courses;
    private List<StudentCertificateItem> certificates;
}
