package com.example.SmartLearningPlatformBackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/lessons")
public class LessonRecapController {

    @Value("${ai.service.uploads-dir:}")
    private String aiServiceUploadsDir;

    // ── PNG recap card ────────────────────────────────────────────────────────

    @GetMapping("/recap-image")
    public ResponseEntity<byte[]> getRecapImage(@RequestParam String path) {
        File file = resolveFile(path, "/recap-cards/");
        if (file == null)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!file.exists())
            return ResponseEntity.notFound().build();

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .contentLength(bytes.length)
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── MP4 recap video (with HTTP range support for browser playback) ────────

    @GetMapping("/recap-video")
    public ResponseEntity<byte[]> getRecapVideo(
            @RequestParam String path,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        File file = resolveFile(path, "/recap-videos/");
        if (file == null)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!file.exists())
            return ResponseEntity.notFound().build();

        long fileSize = file.length();
        long start = 0;
        long end = fileSize - 1;

        // Parse Range: bytes=start-end
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String range = rangeHeader.substring(6);
            String[] parts = range.split("-");
            try {
                start = Long.parseLong(parts[0].trim());
                end = (parts.length > 1 && !parts[1].trim().isEmpty())
                        ? Long.parseLong(parts[1].trim())
                        : fileSize - 1;
            } catch (NumberFormatException ignored) {
            }
        }

        end = Math.min(end, fileSize - 1);
        long contentLength = end - start + 1;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            byte[] data = new byte[(int) contentLength];
            raf.readFully(data);

            boolean isPartial = rangeHeader != null;
            HttpStatus status = isPartial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;

            return ResponseEntity.status(status)
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE,
                            "bytes " + start + "-" + end + "/" + fileSize)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .body(data);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Shared path resolution + guard ───────────────────────────────────────

    private File resolveFile(String rawPath, String requiredSegment) {
        Path raw = Paths.get(rawPath);
        Path resolved;
        if (raw.isAbsolute()) {
            resolved = raw.normalize();
        } else if (aiServiceUploadsDir != null && !aiServiceUploadsDir.isBlank()) {
            resolved = Paths.get(aiServiceUploadsDir).resolve(raw).normalize();
        } else {
            resolved = raw.normalize().toAbsolutePath();
        }
        // Normalise to forward slashes for cross-platform segment check
        String s = resolved.toString().replace('\\', '/');
        // Strip the leading slash from the required segment when comparing
        String seg = requiredSegment.replace('\\', '/');
        if (!s.contains(seg))
            return null;
        return resolved.toFile();
    }
}
