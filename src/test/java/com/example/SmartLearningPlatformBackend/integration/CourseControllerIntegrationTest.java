package com.example.SmartLearningPlatformBackend.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SmartLearningPlatformBackend.controller.CourseController;
import com.example.SmartLearningPlatformBackend.dto.course.CourseDetailResponse;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CourseControllerIntegrationTest {

    @Mock
    private CourseService courseService;

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
                .standaloneSetup(new CourseController(courseService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(new UserDetailsImpl(student)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getCourse_returnsCourseDetailForAuthenticatedStudent() throws Exception {
        when(courseService.getCourseById(20L, 1L)).thenReturn(CourseDetailResponse.builder()
                .id(20L)
                .title("Algorithms")
                .category("Informatique")
                .description("Generated course")
                .totalLessons(5)
                .lessons(List.of())
                .build());

        mockMvc.perform(get("/api/courses/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.title").value("Algorithms"))
                .andExpect(jsonPath("$.totalLessons").value(5));
    }
}
