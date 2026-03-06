package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.notification.NotificationDTO;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.NotificationService;
import com.example.SmartLearningPlatformBackend.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetailsImpl principal) {
        return sseService.subscribe(principal.getUser().getId());
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                notificationService.getUserNotifications(principal.getUser().getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        long count = notificationService.getUnreadCount(principal.getUser().getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }
}
