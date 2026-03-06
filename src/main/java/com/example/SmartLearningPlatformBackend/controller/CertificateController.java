package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.certificate.CertificateDTO;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

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
     * GET /api/certificates/{certificateId}/download — download PDF bytes.
     * This endpoint is permitAll so it can be opened directly in a browser tab.
     * Ownership check is skipped; the certificate ID is enough to identify the
     * file.
     */
    @GetMapping("/{certificateId}/download")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long certificateId) throws IOException {

        byte[] pdfBytes = certificateService.downloadPdfPublic(certificateId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"certificate-" + certificateId + ".pdf\"")
                .body(pdfBytes);
    }
}
