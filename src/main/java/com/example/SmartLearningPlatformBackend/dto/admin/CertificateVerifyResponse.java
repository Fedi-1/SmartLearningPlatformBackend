// src/main/java/com/example/SmartLearningPlatformBackend/dto/admin/CertificateVerifyResponse.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificateVerifyResponse {
    private boolean valid;
    private String studentName;
    private String courseTitle;
    private Integer score;
    private String issuedAt;
    private CertificateStatus status;
}
