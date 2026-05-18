package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.Document;
import com.example.SmartLearningPlatformBackend.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByStudentId(Long studentId);

    @Query(value = """
            select *
            from documents
            where file_hash = :fileHash
            order by id asc
            limit 1
            """, nativeQuery = true)
    Optional<Document> FindByFileHash(@Param("fileHash") String fileHash);

    @Query("""
            select count(d) > 0
            from Document d
            where d.studentId = :studentId
              and d.fileHash = :fileHash
              and d.status <> :status
            """)
    boolean CheckDuplicateDocument(
            @Param("studentId") Long studentId,
            @Param("fileHash") String fileHash,
            @Param("status") DocumentStatus status);

    Optional<Document> findByStudentIdAndFileHash(Long studentId, String fileHash);
}
