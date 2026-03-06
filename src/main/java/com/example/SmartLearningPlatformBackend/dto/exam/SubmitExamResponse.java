package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitExamResponse {
    private Long attemptId;
    private Integer score;
    private Boolean isPassed;
    private Integer totalPointsEarned;
    private Integer totalPointsPossible;
    private Integer attemptNumber;
    private String certificateUuid;
    private Long certificateId;
}
