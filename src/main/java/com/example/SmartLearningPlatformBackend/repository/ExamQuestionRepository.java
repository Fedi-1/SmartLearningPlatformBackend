package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    List<ExamQuestion> findByExamId(Long examId);

    List<ExamQuestion> findByExamIdAndSectionNumber(Long examId, Integer sectionNumber);
}
