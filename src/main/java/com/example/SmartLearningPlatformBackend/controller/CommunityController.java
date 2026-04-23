// src/main/java/com/example/SmartLearningPlatformBackend/controller/CommunityController.java
package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.community.ConversationDTO;
import com.example.SmartLearningPlatformBackend.dto.community.MessageDTO;
import com.example.SmartLearningPlatformBackend.dto.community.SendMessageRequest;
import com.example.SmartLearningPlatformBackend.dto.community.StudentSearchResult;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                communityService.getMyConversations(principal.getUser().getId()));
    }

    @GetMapping("/conversations/{otherStudentId}")
    public ResponseEntity<ConversationDTO> getOrCreateConversation(
            @PathVariable Long otherStudentId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                communityService.getOrCreateConversation(principal.getUser().getId(), otherStudentId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                communityService.getMessages(conversationId, principal.getUser().getId()));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                communityService.sendMessage(principal.getUser().getId(), request));
    }

    @GetMapping("/students/search")
    public ResponseEntity<List<StudentSearchResult>> searchStudents(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                communityService.searchStudents(query, principal.getUser().getId()));
    }
}
