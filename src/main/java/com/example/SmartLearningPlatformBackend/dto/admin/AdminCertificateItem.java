// src/main/java/com/example/SmartLearningPlatformBackend/dto/admin/AdminCertificateItem.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCertificateItem {
    private Long id;
    private String certificateUUID;
    private String studentName;
    private String studentEmail;
    private Long studentId;
    private String courseTitle;
    private Long courseId;
    private String category;
    private Integer score;
    private String issuedAt;
    private CertificateStatus status;
}
