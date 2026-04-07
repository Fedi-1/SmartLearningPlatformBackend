package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.study.StartStudySessionRequest;
import com.example.SmartLearningPlatformBackend.dto.study.StudySessionRequest;
import com.example.SmartLearningPlatformBackend.dto.study.StudySessionResponse;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;

    @PostMapping("/start")
    public ResponseEntity<StudySessionResponse> start(
            @RequestBody StartStudySessionRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Long studentId = principal.getUser().getId();
        return ResponseEntity
                .ok(studySessionService.startSession(studentId, request.getCourseId(), request.getLessonId()));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<StudySessionResponse> heartbeat(
            @RequestBody StudySessionRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Long studentId = principal.getUser().getId();
        return ResponseEntity.ok(studySessionService.heartbeat(studentId, request.getSessionId()));
    }

    @PostMapping("/stop")
    public ResponseEntity<StudySessionResponse> stop(
            @RequestBody StudySessionRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Long studentId = principal.getUser().getId();
        return ResponseEntity.ok(studySessionService.stopSession(studentId, request.getSessionId()));
    }
}
