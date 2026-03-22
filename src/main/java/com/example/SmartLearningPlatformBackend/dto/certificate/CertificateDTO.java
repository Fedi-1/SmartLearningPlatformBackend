package com.example.SmartLearningPlatformBackend.dto.certificate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateDTO {
    private Long id;
    private String certificateUuid;
    private Long courseId;
    private String courseTitle;
    private Integer score;
    private LocalDateTime issuedAt;
    private boolean hasPdf;
    private String status;
}
