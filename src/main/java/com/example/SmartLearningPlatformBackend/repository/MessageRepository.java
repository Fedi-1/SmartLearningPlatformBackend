// src/main/java/com/example/SmartLearningPlatformBackend/repository/MessageRepository.java
package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    int countByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long senderId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.senderId != :readerId")
    int markAllAsReadForRecipient(@Param("conversationId") Long conversationId, @Param("readerId") Long readerId);
}
