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

                if (certificate.getStatus() == CertificateStatus.PENDING) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .header(HttpHeaders.CONTENT_TYPE,
                                                        "text/html; charset=UTF-8")
                                        .body(buildHtmlPage(
                                                        "⏳",
                                                        "Certificate Pending Approval",
                                                        "This certificate is currently awaiting " +
                                                                        "admin approval. You will be notified " +
                                                                        "once it has been reviewed.")
                                                        .getBytes());
                }

                if (certificate.getStatus() == CertificateStatus.REVOKED) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .header(HttpHeaders.CONTENT_TYPE,
                                                        "text/html; charset=UTF-8")
                                        .body(buildHtmlPage(
                                                        "❌",
                                                        "Certificate Revoked",
                                                        "This certificate has been revoked and " +
                                                                        "is no longer valid. Please contact " +
                                                                        "support for more information.")
                                                        .getBytes());
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

        private String buildHtmlPage(String icon,
                        String title,
                        String message) {
                return """
                                <!DOCTYPE html>
                                <html lang="en">
                                <head>
                                  <meta charset="UTF-8">
                                  <meta name="viewport"
                                        content="width=device-width,
                                        initial-scale=1.0">
                                  <title>LearnAI — %s</title>
                                  <style>
                                    * { margin:0; padding:0;
                                        box-sizing:border-box; }
                                    body {
                                      min-height: 100vh;
                                      display: flex;
                                      align-items: center;
                                      justify-content: center;
                                      background: #0f1117;
                                      font-family: 'Segoe UI', system-ui,
                                                   sans-serif;
                                    }
                                    .card {
                                      background: #1a1d27;
                                      border: 1px solid #2d3148;
                                      border-radius: 20px;
                                      padding: 3rem 2.5rem;
                                      max-width: 480px;
                                      width: 90%%;
                                      text-align: center;
                                      box-shadow: 0 25px 60px
                                                  rgba(0,0,0,0.4);
                                    }
                                    .logo {
                                      font-size: 0.85rem;
                                      font-weight: 700;
                                      color: #6366f1;
                                      letter-spacing: 0.1em;
                                      text-transform: uppercase;
                                      margin-bottom: 2rem;
                                    }
                                    .icon {
                                      font-size: 4rem;
                                      line-height: 1;
                                      margin-bottom: 1.25rem;
                                    }
                                    h1 {
                                      font-size: 1.5rem;
                                      font-weight: 700;
                                      color: #f1f5f9;
                                      margin-bottom: 0.75rem;
                                    }
                                    p {
                                      font-size: 0.95rem;
                                      color: #94a3b8;
                                      line-height: 1.6;
                                      margin-bottom: 1.5rem;
                                    }
                                    .divider {
                                      height: 1px;
                                      background: #2d3148;
                                      margin: 1.5rem 0;
                                    }
                                    .footer {
                                      font-size: 0.75rem;
                                      color: #475569;
                                    }
                                  </style>
                                </head>
                                <body>
                                  <div class="card">
                                    <div class="logo">✦ LearnAI</div>
                                    <div class="icon">%s</div>
                                    <h1>%s</h1>
                                    <p>%s</p>
                                    <div class="divider"></div>
                                    <p class="footer">
                                      © 2026 LearnAI — Smart Learning Platform
                                    </p>
                                  </div>
                                </body>
                                </html>
                                """.formatted(title, icon, title, message);
        }
}
