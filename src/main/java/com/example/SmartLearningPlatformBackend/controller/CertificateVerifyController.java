// src/main/java/com/example/SmartLearningPlatformBackend/controller/CertificateVerifyController.java
package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.admin.CertificateVerifyResponse;
import com.example.SmartLearningPlatformBackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/certificates")
@RequiredArgsConstructor
public class CertificateVerifyController {

    private final AdminService adminService;

    @GetMapping("/verify/{uuid}")
    public ResponseEntity<CertificateVerifyResponse> verifyCertificate(@PathVariable String uuid) {
        return ResponseEntity.ok(adminService.verifyCertificate(uuid));
    }
}
