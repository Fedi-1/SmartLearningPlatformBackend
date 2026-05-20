package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.study.StudySessionResponse;
import com.example.SmartLearningPlatformBackend.models.StudySession;
import com.example.SmartLearningPlatformBackend.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private static final long MAX_ACTIVITY_GAP_SECONDS = 120;

    private final StudySessionRepository studySessionRepository;

    @Transactional
    public StudySessionResponse startStudySession(Long studentId, Long courseId, Long lessonId) {
        if (courseId == null || lessonId == null) {
            throw new IllegalArgumentException("courseId and lessonId are required.");
        }

        closeActiveStudySessions(studentId);

        LocalDateTime now = LocalDateTime.now();
        StudySession session = StudySession.builder()
                .studentId(studentId)
                .courseId(courseId)
                .lessonId(lessonId)
                .startedAt(now)
                .lastActivityAt(now)
                .totalActiveSeconds(0L)
                .active(true)
                .build();

        StudySession saved = studySessionRepository.save(session);
        return toResponse(saved);
    }

    @Transactional
    public StudySessionResponse keepSessionActive(Long studentId, Long sessionId) {
        StudySession session = studySessionRepository.findByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Study session not found."));

        if (!Boolean.TRUE.equals(session.getActive())) {
            return toResponse(session);
        }

        recordElapsedActiveTime(session, LocalDateTime.now());
        return toResponse(studySessionRepository.save(session));
    }

    @Transactional
    public StudySessionResponse stopStudySession(Long studentId, Long sessionId) {
        StudySession session = studySessionRepository.findByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Study session not found."));

        if (Boolean.TRUE.equals(session.getActive())) {
            recordElapsedActiveTime(session, LocalDateTime.now());
            session.setEndedAt(LocalDateTime.now());
            session.setActive(false);
            session = studySessionRepository.save(session);
        }

        return toResponse(session);
    }

    @Transactional
    public void closeActiveStudySessions(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        for (StudySession active : studySessionRepository.findByStudentIdAndActiveTrue(studentId)) {
            recordElapsedActiveTime(active, now);
            active.setEndedAt(now);
            active.setActive(false);
            studySessionRepository.save(active);
        }
    }

    @Transactional(readOnly = true)
    public int getTotalStudyMinutes(Long studentId) {
        long accumulated = studySessionRepository.sumTotalActiveSecondsByStudentId(studentId);
        LocalDateTime now = LocalDateTime.now();

        for (StudySession active : studySessionRepository.findByStudentIdAndActiveTrue(studentId)) {
            LocalDateTime lastActivity = active.getLastActivityAt();
            if (lastActivity == null) {
                continue;
            }
            long delta = ChronoUnit.SECONDS.between(lastActivity, now);
            if (delta > 0) {
                accumulated += Math.min(delta, MAX_ACTIVITY_GAP_SECONDS);
            }
        }

        return (int) (accumulated / 60);
    }

    private void recordElapsedActiveTime(StudySession session, LocalDateTime now) {
        LocalDateTime lastActivity = session.getLastActivityAt();
        if (lastActivity == null) {
            session.setLastActivityAt(now);
            return;
        }

        long delta = ChronoUnit.SECONDS.between(lastActivity, now);
        if (delta > 0) {
            long safeDelta = Math.min(delta, MAX_ACTIVITY_GAP_SECONDS);
            session.setTotalActiveSeconds(session.getTotalActiveSeconds() + safeDelta);
        }
        session.setLastActivityAt(now);
    }

    private StudySessionResponse toResponse(StudySession session) {
        return StudySessionResponse.builder()
                .sessionId(session.getId())
                .active(Boolean.TRUE.equals(session.getActive()))
                .totalActiveSeconds(session.getTotalActiveSeconds() == null ? 0L : session.getTotalActiveSeconds())
                .build();
    }
}
