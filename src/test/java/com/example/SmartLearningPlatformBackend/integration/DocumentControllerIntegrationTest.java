package com.example.SmartLearningPlatformBackend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SmartLearningPlatformBackend.controller.DocumentController;
import com.example.SmartLearningPlatformBackend.dto.document.DocumentResponse;
import com.example.SmartLearningPlatformBackend.dto.document.UploadResponse;
import com.example.SmartLearningPlatformBackend.enums.DocumentStatus;
import com.example.SmartLearningPlatformBackend.enums.FileType;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentControllerIntegrationTest {

    @Mock
    private DocumentService documentService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Student student = Student.builder()
                .id(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@mail.test")
                .password("password")
                .role(UserRole.STUDENT)
                .build();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DocumentController(documentService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(new UserDetailsImpl(student)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void uploadDocument_returnsGeneratedCourseSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lesson.pdf",
                "application/pdf",
                "pdf-content".getBytes());

        when(documentService.uploadAndGenerate(any(MultipartFile.class), any(Student.class)))
                .thenReturn(UploadResponse.builder()
                        .documentId(10L)
                        .courseId(20L)
                        .courseTitle("Algorithms")
                        .totalLessons(5)
                        .build());

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(10))
                .andExpect(jsonPath("$.courseId").value(20))
                .andExpect(jsonPath("$.courseTitle").value("Algorithms"));
    }

    @Test
    void getMyDocuments_returnsAuthenticatedStudentDocuments() throws Exception {
        when(documentService.getStudentDocuments(eq(1L))).thenReturn(List.of(DocumentResponse.builder()
                .id(10L)
                .fileName("lesson.pdf")
                .fileType(FileType.PDF)
                .fileSize(1024L)
                .status(DocumentStatus.COMPLETED)
                .courseId(20L)
                .category("Informatique")
                .build()));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].fileName").value("lesson.pdf"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
