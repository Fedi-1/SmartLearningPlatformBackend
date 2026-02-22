package com.example.SmartLearningPlatformBackend.dto.document;

import com.example.SmartLearningPlatformBackend.enums.DocumentStatus;
import com.example.SmartLearningPlatformBackend.enums.FileType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {

    private Long id;
    private String fileName;
    private FileType fileType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private DocumentStatus status;
    private Long courseId;
    private String category;
}
