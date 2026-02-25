package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByExamAttemptId(Long examAttemptId);

    boolean existsByExamAttemptId(Long examAttemptId);
}
