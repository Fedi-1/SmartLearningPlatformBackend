package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.ai.AiCourseResponse;
import com.example.SmartLearningPlatformBackend.enums.FileType;
import com.example.SmartLearningPlatformBackend.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    public AiCourseResponse processDocument(MultipartFile file, FileType fileType) {
        try {
            String url = aiServiceBaseUrl + "/process-document?fileType=" + fileType.name();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<AiCourseResponse> response = restTemplate.postForEntity(
                    url, requestEntity, AiCourseResponse.class);

            if (response.getBody() == null) {
                throw new AiServiceException("AI service returned an empty response.");
            }

            return response.getBody();

        } catch (ResourceAccessException e) {
            throw new AiServiceException("AI service is unreachable. Please ensure it is running on port 8000.");
        } catch (IOException e) {
            throw new AiServiceException("Failed to read uploaded file: " + e.getMessage());
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("AI service error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> generateQuizQuestions(String lessonContent) {
        try {
            String url = aiServiceBaseUrl + "/api/generate-quiz";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("lessonContent", lessonContent);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<List<?>> response = restTemplate.postForEntity(url, requestEntity,
                    (Class<List<?>>) (Class<?>) List.class);

            if (response.getBody() == null) {
                throw new AiServiceException("AI service returned empty quiz questions.");
            }

            return (List<Map<String, Object>>) response.getBody();

        } catch (ResourceAccessException e) {
            throw new AiServiceException("AI service is unreachable. Please ensure it is running on port 8000.");
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("AI quiz generation error: " + e.getMessage());
        }
    }
}
