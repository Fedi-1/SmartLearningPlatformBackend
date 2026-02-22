package com.example.SmartLearningPlatformBackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiCourseResponse {

    @JsonProperty("courseTitle")
    private String courseTitle;

    @JsonProperty("category")
    private String category;

    @JsonProperty("totalLessons")
    private Integer totalLessons;

    @JsonProperty("lessons")
    private List<LessonDto> lessons;
}
