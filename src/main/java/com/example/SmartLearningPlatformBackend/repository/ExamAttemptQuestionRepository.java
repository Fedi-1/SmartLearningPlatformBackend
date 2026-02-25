package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.ExamAttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamAttemptQuestionRepository extends JpaRepository<ExamAttemptQuestion, Long> {

    List<ExamAttemptQuestion> findByExamAttemptId(Long examAttemptId);
}
