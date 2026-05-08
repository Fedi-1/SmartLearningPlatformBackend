// C:\Users\firas\Desktop\PFE Project\SmartLearningPlatformBackend\src\main\java\com\example\SmartLearningPlatformBackend\service\GamificationService.java
package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.gamification.StudentProfileDTO;
import com.example.SmartLearningPlatformBackend.dto.gamification.XpUpdateResponse;
import com.example.SmartLearningPlatformBackend.enums.ActionType;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.enums.Rank;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.ActivityLog;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.repository.ActivityLogRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private static final Map<ActionType, Integer> XP_REWARDS;

    static {
        EnumMap<ActionType, Integer> rewards = new EnumMap<>(ActionType.class);
        rewards.put(ActionType.COMPLETE_LESSON, 10);
        rewards.put(ActionType.PASS_QUIZ, 20);
        rewards.put(ActionType.PASS_EXAM, 50);
        rewards.put(ActionType.FAIL_EXAM, 5);
        rewards.put(ActionType.UPLOAD_DOCUMENT, 5);
        rewards.put(ActionType.GENERATE_COURSE, 15);
        rewards.put(ActionType.DOWNLOAD_CERTIFICATE, 10);
        rewards.put(ActionType.SHARE_COURSE, 15);
        rewards.put(ActionType.JOIN_GROUP, 10);
        rewards.put(ActionType.COMPLETE_CHALLENGE, 30);
        rewards.put(ActionType.DAILY_LOGIN, 5);
        XP_REWARDS = Map.copyOf(rewards);
    }

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;

    @Transactional
    public XpUpdateResponse awardXp(Long userId, ActionType action) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Integer reward = XP_REWARDS.get(action);
        if (reward == null) {
            return null;
        }

        log.info("Awarding {} XP to user {} for {}", reward, userId, action);

        int currentXp = user.getXpPoints() != null ? user.getXpPoints() : 0;
        int previousXp = currentXp;
        Rank previousRank = Rank.fromXp(previousXp);

        int newXp = previousXp + reward;
        user.setXpPoints(newXp);

        Rank newRank = Rank.fromXp(newXp);
        boolean rankUp = newRank != previousRank;

        if (rankUp) {
            notificationService.notify(
                    userId,
                    NotificationCategory.COURSE_COMPLETE,
                    "Rank Up! 🎉",
                    "Congratulations! You reached " + newRank.name() + " rank!",
                    null,
                    "/dashboard/profile");
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastActivityDate = user.getLastActivityDate();

        int currentStreak = safeInt(user.getCurrentStreak());
        if (lastActivityDate == null || lastActivityDate.isBefore(yesterday)) {
            currentStreak = 1;
        } else if (lastActivityDate.equals(yesterday)) {
            currentStreak = currentStreak + 1;
        }

        user.setCurrentStreak(currentStreak);

        int longestStreak = safeInt(user.getLongestStreak());
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
            user.setLongestStreak(longestStreak);
        }

        user.setLastActivityDate(today);
        User savedUser = userRepository.save(user);

        log.info("User {} now has {} XP, rank: {}, streak: {}",
                userId,
                savedUser.getXpPoints(),
                Rank.fromXp(safeInt(savedUser.getXpPoints())).name(),
                savedUser.getCurrentStreak());

        return XpUpdateResponse.builder()
                .xpEarned(reward)
                .totalXp(safeInt(savedUser.getXpPoints()))
                .rank(newRank.name())
                .previousRank(rankUp ? previousRank.name() : null)
                .rankUp(rankUp)
                .currentStreak(safeInt(savedUser.getCurrentStreak()))
                .longestStreak(safeInt(savedUser.getLongestStreak()))
                .build();
    }

    public StudentProfileDTO getStudentProfile(Long studentId, Long currentUserId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Profile not available for this user.");
        }

        Rank currentRank = Rank.fromXp(safeInt(user.getXpPoints()));
        Rank nextRank = findNextRank(currentRank);

        int xpPoints = safeInt(user.getXpPoints());
        int nextRankMinXp = nextRank != null ? nextRank.getMinXp() : -1;
        int xpToNextRank = nextRank != null ? Math.max(0, nextRank.getMinXp() - xpPoints) : 0;

        int completedCourses = (int) activityLogRepository.countByUserIdAndAction(studentId,
                ActionType.GENERATE_COURSE);
        int passedExams = (int) activityLogRepository.countByUserIdAndAction(studentId, ActionType.PASS_EXAM);
        int earnedCertificates = (int) activityLogRepository.countByUserIdAndAction(studentId,
                ActionType.DOWNLOAD_CERTIFICATE);

        List<String> recentAchievements = activityLogRepository.findTop5ByUserIdOrderByTimestampDesc(studentId)
                .stream()
                .map(ActivityLog::getAction)
                .filter(java.util.Objects::nonNull)
                .map(Enum::name)
                .toList();

        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        return StudentProfileDTO.builder()
                .id(user.getId())
                .firstName(firstName)
                .lastName(lastName)
                .fullName(buildFullName(firstName, lastName))
                .initials(buildInitials(firstName, lastName))
                .xpPoints(xpPoints)
                .rank(currentRank.name())
                .rankMinXp(currentRank.getMinXp())
                .nextRankMinXp(nextRankMinXp)
                .xpToNextRank(xpToNextRank)
                .currentStreak(safeInt(user.getCurrentStreak()))
                .longestStreak(safeInt(user.getLongestStreak()))
                .weeklyChampionCount(safeInt(user.getWeeklyChampionCount()))
                .completedCourses(completedCourses)
                .passedExams(passedExams)
                .earnedCertificates(earnedCertificates)
                .recentAchievements(recentAchievements)
                .build();
    }

    public void updateDailyLoginStreak(Long userId) {
        awardXp(userId, ActionType.DAILY_LOGIN);
    }

    public List<StudentProfileDTO> getLeaderboard(int limit) {
        int safeLimit = limit > 0 ? limit : 10;

        return userRepository.findByRole(UserRole.STUDENT)
                .stream()
                .sorted(Comparator.comparingInt((User u) -> safeInt(u.getXpPoints())).reversed())
                .limit(safeLimit)
                .map(user -> {
                    try {
                        return getStudentProfile(user.getId(), user.getId());
                    } catch (Exception e) {
                        log.warn("Failed to build profile for user {}: {}", user.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(p -> p != null)
                .toList();
    }

    private Rank findNextRank(Rank currentRank) {
        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i] == currentRank) {
                return (i + 1 < ranks.length) ? ranks[i + 1] : null;
            }
        }
        return null;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        return (first + " " + last).trim();
    }

    private String buildInitials(String firstName, String lastName) {
        StringBuilder initials = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            initials.append(Character.toUpperCase(firstName.trim().charAt(0)));
        }
        if (lastName != null && !lastName.isBlank()) {
            initials.append(Character.toUpperCase(lastName.trim().charAt(0)));
        }
        return initials.toString();
    }
}
