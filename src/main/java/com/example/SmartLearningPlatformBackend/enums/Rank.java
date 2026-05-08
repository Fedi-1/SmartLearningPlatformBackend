package com.example.SmartLearningPlatformBackend.enums;

public enum Rank {
    BEGINNER(0),
    LEARNER(200),
    SCHOLAR(500),
    EXPERT(1000),
    MASTER(2000);

    private final int minXp;

    Rank(int minXp) {
        this.minXp = minXp;
    }

    public int getMinXp() {
        return minXp;
    }

    public static Rank fromXp(int xp) {
        Rank result = BEGINNER;
        for (Rank rank : values()) {
            if (xp >= rank.minXp) {
                result = rank;
            }
        }
        return result;
    }
}
