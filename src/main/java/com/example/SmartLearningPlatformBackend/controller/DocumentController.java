package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.document.DocumentResponse;
import com.example.SmartLearningPlatformBackend.dto.document.UploadResponse;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.DocumentService;
import com.example.SmartLearningPlatformBackend.exception.DuplicateDocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Student student = (Student) principal.getUser();
        UploadResponse response = documentService.uploadAndGenerate(file, student);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        List<DocumentResponse> docs = documentService.getStudentDocuments(principal.getUser().getId());
        return ResponseEntity.ok(docs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        documentService.softDeleteDocument(id, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateDocumentException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }
}
