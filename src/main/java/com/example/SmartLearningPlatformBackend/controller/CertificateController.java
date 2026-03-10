// src/main/java/com/example/SmartLearningPlatformBackend/controller/CertificateController.java
package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.certificate.CertificateDTO;
import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

        private final CertificateService certificateService;
        private final CertificateRepository certificateRepository;

        /** GET /api/certificates/my-certificates */
        @GetMapping("/my-certificates")
        public ResponseEntity<List<CertificateDTO>> getMyCertificates(
                        @AuthenticationPrincipal UserDetailsImpl principal) {
                return ResponseEntity.ok(
                                certificateService.getMyCertificates(principal.getUser().getId()));
        }

        /** GET /api/certificates/course/{courseId} */
        @GetMapping("/course/{courseId}")
        public ResponseEntity<CertificateDTO> getCourseCertificate(
                        @PathVariable Long courseId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {
                return ResponseEntity.ok(
                                certificateService.getCourseCertificate(principal.getUser().getId(), courseId));
        }

        /** POST /api/certificates/{certificateId}/generate — generate PDF */
        @PostMapping("/{certificateId}/generate")
        public ResponseEntity<CertificateDTO> generatePdf(
                        @PathVariable Long certificateId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {
                return ResponseEntity.ok(
                                certificateService.generatePdf(certificateId, principal.getUser().getId()));
        }

        /**
         * GET /api/certificates/{uuid}/download — public, no auth required.
         * Looks up by certificateUUID and returns the PDF bytes directly.
         */
        @GetMapping("/{uuid}/download")
        public ResponseEntity<byte[]> downloadByUuid(@PathVariable String uuid) {
                Certificate certificate = certificateRepository.findByCertificateUuid(uuid)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Certificate not found"));

                if (certificate.getStatus() != CertificateStatus.APPROVED) {
                        String message = certificate.getStatus() == CertificateStatus.PENDING
                                        ? "This certificate is awaiting admin approval."
                                        : "This certificate has been revoked.";
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(message.getBytes());
                }

                if (certificate.getPdfContent() == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate PDF not available");
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"certificate_" + certificate.getCertificateUuid()
                                                                + ".pdf\"")
                                .body(certificate.getPdfContent());
        }
}
