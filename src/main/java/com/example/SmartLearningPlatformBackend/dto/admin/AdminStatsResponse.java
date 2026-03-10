// src/main/java/.../dto/admin/AdminStatsResponse.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalStudents;
    private long totalCourses;
    private long totalCertificates;
    private long totalDocuments;
    private int examPassRate;
}
