package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContentItem {
    private Long documentId;
    private String documentFileName;
    private String documentFileType;
    private String documentStatus;
    private Long documentFileSize;
    private String documentUploadedAt;
    private String documentCategory;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long courseId;
    private String courseTitle;
    private String courseCategory;
    private Integer totalLessons;
}
