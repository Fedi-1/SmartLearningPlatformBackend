// src/main/java/com/example/SmartLearningPlatformBackend/dto/community/ConversationDTO.java
package com.example.SmartLearningPlatformBackend.dto.community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {

    private Long id;
    private Long otherStudentId;
    private String otherStudentName;
    private String otherStudentInitials;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
    private LocalDateTime createdAt;
}
