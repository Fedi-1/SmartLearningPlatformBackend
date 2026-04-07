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

    private static final long MAX_HEARTBEAT_DELTA_SECONDS = 120;

    private final StudySessionRepository studySessionRepository;

    @Transactional
    public StudySessionResponse startSession(Long studentId, Long courseId, Long lessonId) {
        if (courseId == null || lessonId == null) {
            throw new IllegalArgumentException("courseId and lessonId are required.");
        }

        closeAllActiveSessions(studentId);

        LocalDateTime now = LocalDateTime.now();
        StudySession session = StudySession.builder()
                .studentId(studentId)
                .courseId(courseId)
                .lessonId(lessonId)
                .startedAt(now)
                .lastHeartbeatAt(now)
                .accumulatedSeconds(0L)
                .isActive(true)
                .build();

        StudySession saved = studySessionRepository.save(session);
        return toResponse(saved);
    }

    @Transactional
    public StudySessionResponse heartbeat(Long studentId, Long sessionId) {
        StudySession session = studySessionRepository.findByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Study session not found."));

        if (!Boolean.TRUE.equals(session.getIsActive())) {
            return toResponse(session);
        }

        applyElapsedSeconds(session, LocalDateTime.now());
        return toResponse(studySessionRepository.save(session));
    }

    @Transactional
    public StudySessionResponse stopSession(Long studentId, Long sessionId) {
        StudySession session = studySessionRepository.findByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Study session not found."));

        if (Boolean.TRUE.equals(session.getIsActive())) {
            applyElapsedSeconds(session, LocalDateTime.now());
            session.setEndedAt(LocalDateTime.now());
            session.setIsActive(false);
            session = studySessionRepository.save(session);
        }

        return toResponse(session);
    }

    @Transactional
    public void closeAllActiveSessions(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        for (StudySession active : studySessionRepository.findByStudentIdAndIsActiveTrue(studentId)) {
            applyElapsedSeconds(active, now);
            active.setEndedAt(now);
            active.setIsActive(false);
            studySessionRepository.save(active);
        }
    }

    @Transactional(readOnly = true)
    public int getTotalStudyMinutes(Long studentId) {
        long accumulated = studySessionRepository.sumAccumulatedSecondsByStudentId(studentId);
        LocalDateTime now = LocalDateTime.now();

        for (StudySession active : studySessionRepository.findByStudentIdAndIsActiveTrue(studentId)) {
            LocalDateTime lastHeartbeat = active.getLastHeartbeatAt();
            if (lastHeartbeat == null) {
                continue;
            }
            long delta = ChronoUnit.SECONDS.between(lastHeartbeat, now);
            if (delta > 0) {
                accumulated += Math.min(delta, MAX_HEARTBEAT_DELTA_SECONDS);
            }
        }

        return (int) (accumulated / 60);
    }

    private void applyElapsedSeconds(StudySession session, LocalDateTime now) {
        LocalDateTime lastHeartbeat = session.getLastHeartbeatAt();
        if (lastHeartbeat == null) {
            session.setLastHeartbeatAt(now);
            return;
        }

        long delta = ChronoUnit.SECONDS.between(lastHeartbeat, now);
        if (delta > 0) {
            long safeDelta = Math.min(delta, MAX_HEARTBEAT_DELTA_SECONDS);
            session.setAccumulatedSeconds(session.getAccumulatedSeconds() + safeDelta);
        }
        session.setLastHeartbeatAt(now);
    }

    private StudySessionResponse toResponse(StudySession session) {
        return StudySessionResponse.builder()
                .sessionId(session.getId())
                .active(Boolean.TRUE.equals(session.getIsActive()))
                .accumulatedSeconds(session.getAccumulatedSeconds() == null ? 0L : session.getAccumulatedSeconds())
                .build();
    }
}
