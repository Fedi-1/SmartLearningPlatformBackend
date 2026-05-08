// C:\Users\firas\Desktop\PFE Project\SmartLearningPlatformBackend\src\main\java\com\example\SmartLearningPlatformBackend\controller\GamificationController.java
package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.gamification.StudentProfileDTO;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/profile/{studentId}")
    public ResponseEntity<StudentProfileDTO> getStudentProfile(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                gamificationService.getStudentProfile(studentId, principal.getUser().getId()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<StudentProfileDTO>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(gamificationService.getLeaderboard(limit));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<StudentProfileDTO> getMyProfile(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        if (principal.getUser().getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Gamification is only available for students.");
        }
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(gamificationService.getStudentProfile(userId, userId));
    }
}
