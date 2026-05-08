package com.example.SmartLearningPlatformBackend.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpUpdateResponse {

    private int xpEarned;
    private int totalXp;
    private String rank;
    private String previousRank;
    private boolean rankUp;
    private int currentStreak;
    private int longestStreak;
}
