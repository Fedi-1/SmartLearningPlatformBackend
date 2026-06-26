package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.study.StudySessionResponse;
import com.example.SmartLearningPlatformBackend.models.StudySession;
import com.example.SmartLearningPlatformBackend.repository.StudySessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudySessionServiceTest {

    @Mock
    private StudySessionRepository studySessionRepository;

    @InjectMocks
    private StudySessionService studySessionService;

    @Test
    void startStudySession_requiresCourseAndLesson() {
        assertThrows(IllegalArgumentException.class, () -> studySessionService.startStudySession(1L, null, 2L));
    }

    @Test
    void startStudySession_closesExistingActiveSessionAndCreatesNewOne() {
        StudySession active = StudySession.builder()
                .id(1L)
                .studentId(7L)
                .courseId(10L)
                .lessonId(20L)
                .lastActivityAt(LocalDateTime.now().minusSeconds(30))
                .totalActiveSeconds(10L)
                .active(true)
                .build();

        when(studySessionRepository.findByStudentIdAndActiveTrue(7L)).thenReturn(List.of(active));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> {
            StudySession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(99L);
            }
            return session;
        });

        StudySessionResponse response = studySessionService.startStudySession(7L, 11L, 22L);

        assertFalse(active.getActive());
        assertEquals(99L, response.getSessionId());
        assertTrue(response.isActive());
    }

    @Test
    void keepSessionActive_capsElapsedTimeAtTwoMinutes() {
        StudySession session = StudySession.builder()
                .id(3L)
                .studentId(7L)
                .lastActivityAt(LocalDateTime.now().minusMinutes(10))
                .totalActiveSeconds(5L)
                .active(true)
                .build();

        when(studySessionRepository.findByIdAndStudentId(3L, 7L)).thenReturn(Optional.of(session));
        when(studySessionRepository.save(session)).thenReturn(session);

        StudySessionResponse response = studySessionService.keepSessionActive(7L, 3L);

        assertEquals(125L, response.getTotalActiveSeconds());
    }

    @Test
    void stopStudySession_marksSessionInactive() {
        StudySession session = StudySession.builder()
                .id(3L)
                .studentId(7L)
                .lastActivityAt(LocalDateTime.now().minusSeconds(20))
                .totalActiveSeconds(0L)
                .active(true)
                .build();

        when(studySessionRepository.findByIdAndStudentId(3L, 7L)).thenReturn(Optional.of(session));
        when(studySessionRepository.save(session)).thenReturn(session);

        StudySessionResponse response = studySessionService.stopStudySession(7L, 3L);

        assertFalse(response.isActive());
        assertFalse(session.getActive());
    }

    @Test
    void getTotalStudyMinutes_includesCappedActiveSessions() {
        StudySession active = StudySession.builder()
                .id(1L)
                .studentId(7L)
                .lastActivityAt(LocalDateTime.now().minusMinutes(10))
                .totalActiveSeconds(0L)
                .active(true)
                .build();

        when(studySessionRepository.sumTotalActiveSecondsByStudentId(7L)).thenReturn(180L);
        when(studySessionRepository.findByStudentIdAndActiveTrue(7L)).thenReturn(List.of(active));

        assertEquals(5, studySessionService.getTotalStudyMinutes(7L));
    }
}
