package com.example.SmartLearningPlatformBackend.dto.lesson;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonProgressResponse {

    private Long id;
    private Long lessonId;
    @JsonProperty("isCompleted")
    private boolean isCompleted;
    @JsonProperty("isLocked")
    private boolean isLocked;
    @JsonProperty("quizPassed")
    private boolean quizPassed;
}
