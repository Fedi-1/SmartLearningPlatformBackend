// src/main/java/com/example/SmartLearningPlatformBackend/dto/community/MessageDTO.java
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
public class MessageDTO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private Boolean isRead;
    private LocalDateTime sentAt;
    private String content;
    private Long sharedCourseId;
    private String sharedCourseTitle;
    private Boolean isMine;
}
