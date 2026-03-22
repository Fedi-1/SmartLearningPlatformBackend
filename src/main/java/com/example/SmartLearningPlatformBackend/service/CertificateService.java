package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.certificate.CertificateDTO;
import com.example.SmartLearningPlatformBackend.enums.ActionType;
import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;
    private final ActivityLogService activityLogService;

    /** Return all certificates earned by this student. */
    public List<CertificateDTO> getMyCertificates(Long studentId) {
        return certificateRepository.findAllByStudentId(studentId).stream()
                .filter(c -> c.getStatus() != CertificateStatus.REVOKED)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Return the certificate for a specific course, if one exists. */
    public CertificateDTO getCourseCertificate(Long studentId, Long courseId) {
        Certificate cert = certificateRepository.findByStudentIdAndCourseId(studentId, courseId)
                .filter(c -> c.getStatus() != CertificateStatus.REVOKED)
                .orElseThrow(() -> new RuntimeException("No certificate found for this course."));
        return toDTO(cert);
    }

    /**
     * Ask the FastAPI service to generate the PDF for this certificate,
     * persist the resulting file path, and return the updated DTO.
     */
    public CertificateDTO generatePdf(Long certificateId, Long studentId) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found."));

        if (!cert.getStudentId().equals(studentId)) {
            throw new RuntimeException("Access denied.");
        }

        // Resolve related data
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found."));
        Course course = courseRepository.findById(cert.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found."));

        // Build payload for FastAPI
        Map<String, Object> payload = new HashMap<>();
        payload.put("certificateUuid", cert.getCertificateUuid());
        payload.put("studentName", user.getFirstName() + " " + user.getLastName());
        payload.put("courseTitle", course.getTitle());
        payload.put("score", cert.getScore());
        payload.put("issuedAt", cert.getIssuedAt().toString());

        Map<String, Object> result = aiServiceClient.generateCertificate(payload);

        String pdfBase64 = (String) result.get("pdfContent");
        if (pdfBase64 != null) {
            byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);
            cert.setPdfContent(pdfBytes);
            certificateRepository.save(cert);
        }

        return toDTO(cert);
    }

    /** Return the PDF bytes stored in PostgreSQL (authenticated). */
    public byte[] downloadPdf(Long certificateId, Long studentId) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found."));

        if (!cert.getStudentId().equals(studentId)) {
            throw new RuntimeException("Access denied.");
        }

        if (cert.getStatus() != CertificateStatus.APPROVED) {
            throw new IllegalStateException(
                    cert.getStatus() == CertificateStatus.PENDING
                            ? "Your certificate is awaiting admin approval."
                            : "Your certificate has been revoked and cannot be downloaded.");
        }

        if (cert.getPdfContent() == null) {
            throw new RuntimeException("PDF has not been generated yet. Please generate it first.");
        }

        activityLogService.log(cert.getStudentId(), ActionType.DOWNLOAD_CERTIFICATE, "Certificate", cert.getId());

        return cert.getPdfContent();
    }

    /**
     * Public download — no ownership check.
     * Used by the browser download link (no JWT header available).
     */
    public byte[] downloadPdfPublic(Long certificateId) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found."));

        if (cert.getStatus() != CertificateStatus.APPROVED) {
            throw new IllegalStateException(
                    cert.getStatus() == CertificateStatus.PENDING
                            ? "This certificate is awaiting admin approval."
                            : "This certificate has been revoked.");
        }

        if (cert.getPdfContent() == null) {
            throw new RuntimeException("PDF has not been generated yet. Please generate it first.");
        }

        activityLogService.log(cert.getStudentId(), ActionType.DOWNLOAD_CERTIFICATE, "Certificate", cert.getId());

        return cert.getPdfContent();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private CertificateDTO toDTO(Certificate cert) {
        String courseTitle = courseRepository.findById(cert.getCourseId())
                .map(Course::getTitle)
                .orElse("Unknown Course");

        return CertificateDTO.builder()
                .id(cert.getId())
                .certificateUuid(cert.getCertificateUuid())
                .courseId(cert.getCourseId())
                .courseTitle(courseTitle)
                .score(cert.getScore())
                .issuedAt(cert.getIssuedAt())
                .hasPdf(cert.getPdfContent() != null)
                .status(cert.getStatus() != null ? cert.getStatus().name() : null)
                .build();
    }
}
