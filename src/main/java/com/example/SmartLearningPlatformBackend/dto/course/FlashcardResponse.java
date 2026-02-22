package com.example.SmartLearningPlatformBackend.dto.course;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlashcardResponse {
    private Long id;
    private String term;
    private String definition;
}
