package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {

    List<ExamAnswer> findByExamAttemptId(Long examAttemptId);

    @Modifying
    @Query("DELETE FROM ExamAnswer ea WHERE ea.examAttemptId IN :attemptIds")
    void deleteAllByExamAttemptIdIn(List<Long> attemptIds);
}
