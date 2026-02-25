package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    boolean existsByStudentIdAndExamIdAndIsPassed(Long studentId, Long examId, Boolean isPassed);

    List<ExamAttempt> findByStudentIdAndExamIdIn(Long studentId, List<Long> examIds);

    List<ExamAttempt> findByStudentIdAndExamId(Long studentId, Long examId);

    int countByStudentIdAndExamId(Long studentId, Long examId);
}
