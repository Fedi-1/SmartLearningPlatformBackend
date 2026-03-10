// src/main/java/com/example/SmartLearningPlatformBackend/dto/admin/StudentCertificateItem.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCertificateItem {
    private Long id;
    private String courseTitle;
    private Integer score;
    private String issuedAt;
    private CertificateStatus status;
}
