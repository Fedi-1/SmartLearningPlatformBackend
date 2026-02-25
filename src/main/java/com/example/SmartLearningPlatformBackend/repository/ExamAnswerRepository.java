package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {

    List<ExamAnswer> findByExamAttemptId(Long examAttemptId);
}
