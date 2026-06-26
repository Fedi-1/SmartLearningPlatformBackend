package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.exam.LogSuspiciousActivityRequest;
import com.example.SmartLearningPlatformBackend.dto.exam.SuspiciousActivityDTO;
import com.example.SmartLearningPlatformBackend.enums.SuspiciousActivityType;
import com.example.SmartLearningPlatformBackend.models.ExamAttempt;
import com.example.SmartLearningPlatformBackend.models.SuspiciousActivity;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.SuspiciousActivityRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuspiciousActivityServiceTest {

    @Mock
    private SuspiciousActivityRepository suspiciousActivityRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @InjectMocks
    private SuspiciousActivityService suspiciousActivityService;

    @Test
    void logActivity_incrementsExistingActivityCount() {
        ExamAttempt attempt = ExamAttempt.builder().id(1L).studentId(7L).build();
        SuspiciousActivity existing = SuspiciousActivity.builder()
                .id(10L)
                .examAttemptId(1L)
                .activityType(SuspiciousActivityType.TAB_SWITCH)
                .count(2)
                .build();

        LogSuspiciousActivityRequest request = new LogSuspiciousActivityRequest();
        request.setActivityType(SuspiciousActivityType.TAB_SWITCH);

        when(examAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(suspiciousActivityRepository.findByExamAttemptIdAndActivityType(1L, SuspiciousActivityType.TAB_SWITCH))
                .thenReturn(Optional.of(existing));
        when(suspiciousActivityRepository.save(existing)).thenReturn(existing);
        doReturn(3).when(suspiciousActivityRepository).totalCountForAttempt(1L);

        SuspiciousActivityDTO response = suspiciousActivityService.logActivity(1L, 7L, request);

        assertEquals(3, existing.getCount());
        assertEquals(3, response.getCount());
        assertEquals(3, response.getTotalCount());
        verify(suspiciousActivityRepository).save(existing);
    }

    @Test
    void logActivity_rejectsAttemptOwnedByAnotherStudent() {
        ExamAttempt attempt = ExamAttempt.builder().id(1L).studentId(7L).build();
        LogSuspiciousActivityRequest request = new LogSuspiciousActivityRequest();
        request.setActivityType(SuspiciousActivityType.COPY_PASTE);

        when(examAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> suspiciousActivityService.logActivity(1L, 99L, request));

        assertEquals("Access denied.", ex.getMessage());
    }
}
