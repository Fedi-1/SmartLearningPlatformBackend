package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.certificate.CertificateDTO;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${ai.service.uploads-dir}")
    private String aiServiceUploadsDir;

    /** Return all certificates earned by this student. */
    public List<CertificateDTO> getMyCertificates(Long studentId) {
        return certificateRepository.findAllByStudentId(studentId).stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsRevoked()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Return the certificate for a specific course, if one exists. */
    public CertificateDTO getCourseCertificate(Long studentId, Long courseId) {
        Certificate cert = certificateRepository.findByStudentIdAndCourseId(studentId, courseId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsRevoked()))
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

        String pdfPath = (String) result.get("pdfFilePath");
        if (pdfPath != null) {
            cert.setPdfFilePath(pdfPath);
            certificateRepository.save(cert);
        }

        return toDTO(cert);
    }

    /** Read the PDF bytes from disk and return them. */
    public byte[] downloadPdf(Long certificateId, Long studentId) throws IOException {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found."));

        if (!cert.getStudentId().equals(studentId)) {
            throw new RuntimeException("Access denied.");
        }

        if (cert.getPdfFilePath() == null) {
            throw new RuntimeException("PDF has not been generated yet. Please generate it first.");
        }

        Path path = resolvePdfPath(cert.getPdfFilePath());
        if (!Files.exists(path)) {
            throw new RuntimeException("Certificate PDF file not found on disk.");
        }

        return Files.readAllBytes(path);
    }

    /**
     * Public download — no ownership check.
     * Used by the browser download link (no JWT header available).
     */
    public byte[] downloadPdfPublic(Long certificateId) throws IOException {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found."));

        if (cert.getPdfFilePath() == null) {
            throw new RuntimeException("PDF has not been generated yet. Please generate it first.");
        }

        Path path = resolvePdfPath(cert.getPdfFilePath());
        if (!Files.exists(path)) {
            throw new RuntimeException("Certificate PDF file not found on disk. Path: " + path);
        }

        return Files.readAllBytes(path);
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
                .hasPdf(cert.getPdfFilePath() != null)
                .build();
    }

    /**
     * Resolve a possibly-relative PDF path against the AI service base directory.
     * FastAPI stores paths like "uploads\certificates\certificate_xxx.pdf" relative
     * to its own working directory (ai.service.uploads-dir).
     */
    private Path resolvePdfPath(String storedPath) {
        Path p = Paths.get(storedPath);
        if (p.isAbsolute())
            return p;
        return Paths.get(aiServiceUploadsDir).resolve(storedPath).normalize();
    }
}
