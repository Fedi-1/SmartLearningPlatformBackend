// src/main/java/com/example/SmartLearningPlatformBackend/repository/ConversationRepository.java
package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByStudentOneIdAndStudentTwoId(Long studentOneId, Long studentTwoId);

    List<Conversation> findByStudentOneIdOrStudentTwoId(Long studentOneId, Long studentTwoId);

    @Query("SELECT c FROM Conversation c WHERE c.studentOneId = :studentId OR c.studentTwoId = :studentId ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findAllForStudentOrderByLastMessageAtDesc(@Param("studentId") Long studentId);
}
