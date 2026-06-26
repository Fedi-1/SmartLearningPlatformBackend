package com.example.SmartLearningPlatformBackend.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SmartLearningPlatformBackend.controller.StudentProfileController;
import com.example.SmartLearningPlatformBackend.dto.student.StudentProfileResponse;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.StudentProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StudentProfileControllerIntegrationTest {

    @Mock
    private StudentProfileService studentProfileService;

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
                .standaloneSetup(new StudentProfileController(studentProfileService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(new UserDetailsImpl(student)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getProfile_returnsAuthenticatedStudentProfile() throws Exception {
        when(studentProfileService.getProfile(1L)).thenReturn(StudentProfileResponse.builder()
                .id(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@mail.test")
                .phoneNumber("12345678")
                .build());

        mockMvc.perform(get("/api/students/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@mail.test"))
                .andExpect(jsonPath("$.phoneNumber").value("12345678"));
    }
}
