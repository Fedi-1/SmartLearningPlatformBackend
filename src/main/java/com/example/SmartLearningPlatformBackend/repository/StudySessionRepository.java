package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    Optional<StudySession> findByIdAndStudentId(Long id, Long studentId);

    List<StudySession> findByStudentIdAndIsActiveTrue(Long studentId);

    @Query("SELECT COALESCE(SUM(s.accumulatedSeconds), 0) FROM StudySession s WHERE s.studentId = :studentId")
    Long sumAccumulatedSecondsByStudentId(@Param("studentId") Long studentId);
}
