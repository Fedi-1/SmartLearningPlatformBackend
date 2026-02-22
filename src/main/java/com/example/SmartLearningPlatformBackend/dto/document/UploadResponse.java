package com.example.SmartLearningPlatformBackend.dto.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {

    private Long documentId;
    private Long courseId;
    private String courseTitle;
    private Integer totalLessons;
}
