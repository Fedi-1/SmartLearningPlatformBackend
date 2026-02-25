package com.example.SmartLearningPlatformBackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LessonDto {

    @JsonProperty("lessonNumber")
    private Integer lessonNumber;

    @JsonProperty("title")
    private String title;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("content")
    private String content;

    @JsonProperty("estimatedReadTime")
    private Integer estimatedReadTime;

    @JsonProperty("recapVideoPath")
    private String recapVideoPath;

    @JsonProperty("flashcards")
    private List<FlashcardDto> flashcards;
}
