package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByExamAttemptId(Long examAttemptId);

    boolean existsByExamAttemptId(Long examAttemptId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<Certificate> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Certificate> findAllByStudentId(Long studentId);

    Optional<Certificate> findByCertificateUuid(String certificateUuid);

    @Modifying
    @Query("DELETE FROM Certificate c WHERE c.examAttemptId IN :attemptIds")
    void deleteAllByExamAttemptIdIn(List<Long> attemptIds);
}
