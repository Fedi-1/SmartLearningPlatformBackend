package com.example.SmartLearningPlatformBackend.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String initials;
    private int xpPoints;
    private String rank;
    private int rankMinXp;
    private int nextRankMinXp;
    private int xpToNextRank;
    private int currentStreak;
    private int longestStreak;
    private int weeklyChampionCount;
    private int completedCourses;
    private int passedExams;
    private int earnedCertificates;
    private List<String> recentAchievements;
}
