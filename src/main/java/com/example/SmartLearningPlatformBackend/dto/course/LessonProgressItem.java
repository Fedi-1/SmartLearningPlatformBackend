package com.example.SmartLearningPlatformBackend.dto.course;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonProgressItem {

    private Long lessonId;
    @JsonProperty("isCompleted")
    private boolean isCompleted;
    @JsonProperty("isLocked")
    private boolean isLocked;
    @JsonProperty("quizPassed")
    private boolean quizPassed;
}
