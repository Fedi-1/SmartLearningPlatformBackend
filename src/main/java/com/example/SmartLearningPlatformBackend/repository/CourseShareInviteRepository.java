// src/main/java/com/example/SmartLearningPlatformBackend/repository/CourseShareInviteRepository.java
package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.CourseShareInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseShareInviteRepository extends JpaRepository<CourseShareInvite, Long> {

    List<CourseShareInvite> findByRecipientIdAndStatus(Long recipientId, String status);

    List<CourseShareInvite> findBySenderIdOrRecipientId(Long senderId, Long recipientId);

    boolean existsBySenderIdAndOriginalCourseIdAndRecipientId(Long senderId, Long originalCourseId, Long recipientId);
}
