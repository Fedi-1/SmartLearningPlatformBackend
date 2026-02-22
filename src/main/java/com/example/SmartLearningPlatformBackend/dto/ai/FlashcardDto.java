package com.example.SmartLearningPlatformBackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FlashcardDto {

    @JsonProperty("term")
    private String term;

    @JsonProperty("definition")
    private String definition;
}
