// src/main/java/com/example/SmartLearningPlatformBackend/dto/community/SendMessageRequest.java
package com.example.SmartLearningPlatformBackend.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    private Long conversationId;

    private Long recipientId;

    @NotBlank(message = "Content is required")
    @Size(max = 1000, message = "Content must be at most 1000 characters")
    private String content;

    private Long courseIdToShare;
}
