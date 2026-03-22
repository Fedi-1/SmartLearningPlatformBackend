package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.Document;
import com.example.SmartLearningPlatformBackend.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByStudentId(Long studentId);

    Optional<Document> findFirstByFileHash(String fileHash);

    boolean existsByStudentIdAndFileHashAndStatusNot(Long studentId, String fileHash, DocumentStatus status);

    Optional<Document> findByStudentIdAndFileHash(Long studentId, String fileHash);
}
