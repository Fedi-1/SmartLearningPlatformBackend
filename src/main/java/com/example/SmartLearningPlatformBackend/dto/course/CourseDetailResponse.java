package com.example.SmartLearningPlatformBackend.dto.course;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CourseDetailResponse {
    private Long id;
    private String title;
    private String description;
    private Integer totalLessons;
    private List<LessonResponse> lessons;
}
