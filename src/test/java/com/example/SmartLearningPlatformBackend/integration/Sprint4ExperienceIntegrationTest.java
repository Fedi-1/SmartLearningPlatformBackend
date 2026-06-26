package com.example.SmartLearningPlatformBackend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SmartLearningPlatformBackend.controller.CommunityController;
import com.example.SmartLearningPlatformBackend.controller.DashboardController;
import com.example.SmartLearningPlatformBackend.controller.LessonProgressController;
import com.example.SmartLearningPlatformBackend.controller.StudySessionController;
import com.example.SmartLearningPlatformBackend.dto.community.MessageDTO;
import com.example.SmartLearningPlatformBackend.dto.community.StudentSearchResult;
import com.example.SmartLearningPlatformBackend.dto.dashboard.DashboardResponse;
import com.example.SmartLearningPlatformBackend.dto.study.StudySessionResponse;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.CommunityService;
import com.example.SmartLearningPlatformBackend.service.DashboardService;
import com.example.SmartLearningPlatformBackend.service.LessonProgressService;
import com.example.SmartLearningPlatformBackend.service.StudySessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class Sprint4ExperienceIntegrationTest {

    @Mock
    private LessonProgressService lessonProgressService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private CommunityService communityService;
    @Mock
    private StudySessionService studySessionService;

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
                .standaloneSetup(
                        new LessonProgressController(lessonProgressService),
                        new DashboardController(dashboardService),
                        new CommunityController(communityService),
                        new StudySessionController(studySessionService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(new UserDetailsImpl(student)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void generateLessonRecap_returnsGeneratedMediaPaths() throws Exception {
        when(lessonProgressService.generateLessonRecap(101L)).thenReturn(Map.of(
                "recapVideoPath", "/api/media/recaps/101.mp4",
                "subtitleEnPath", "/api/media/recaps/101.en.vtt"));

        mockMvc.perform(post("/api/lessons/101/generate-recap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recapVideoPath").value("/api/media/recaps/101.mp4"))
                .andExpect(jsonPath("$.subtitleEnPath").value("/api/media/recaps/101.en.vtt"));
    }

    @Test
    void getDashboard_returnsLearningProgressSummary() throws Exception {
        DashboardResponse response = DashboardResponse.builder()
                .stats(DashboardResponse.StatsDto.builder()
                        .totalCourses(4)
                        .completedCourses(1)
                        .totalLessons(20)
                        .completedLessons(12)
                        .totalQuizAttempts(9)
                        .passedQuizAttempts(7)
                        .averageQuizScore(81)
                        .flashcardsDueToday(6)
                        .totalFlashcards(40)
                        .totalStudyMinutes(135)
                        .build())
                .courses(List.of(DashboardResponse.CourseProgressDto.builder()
                        .courseId(20L)
                        .title("Algorithms")
                        .category("Informatique")
                        .progressPercentage(60)
                        .totalLessons(10)
                        .completedLessons(6)
                        .quizzesPassed(5)
                        .totalQuizzes(8)
                        .examPassed(false)
                        .build()))
                .recentActivity(List.of())
                .flashcardsDue(List.of(DashboardResponse.FlashcardsDueByCourseDto.builder()
                        .courseId(20L)
                        .courseTitle("Algorithms")
                        .dueCount(6)
                        .build()))
                .build();

        when(dashboardService.getDashboardData(1L)).thenReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalStudyMinutes").value(135))
                .andExpect(jsonPath("$.courses[0].progressPercentage").value(60))
                .andExpect(jsonPath("$.flashcardsDue[0].dueCount").value(6));
    }

    @Test
    void searchStudents_returnsMatchingClassmates() throws Exception {
        when(communityService.searchStudents("Grace", 1L)).thenReturn(List.of(StudentSearchResult.builder()
                .id(2L)
                .fullName("Grace Hopper")
                .initials("GH")
                .email("grace@mail.test")
                .build()));

        mockMvc.perform(get("/api/community/students/search").param("query", "Grace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Grace Hopper"));
    }

    @Test
    void sendCommunityMessage_returnsPersistedMessageWithSharedCourse() throws Exception {
        when(communityService.sendMessage(eq(1L), any())).thenReturn(MessageDTO.builder()
                .id(300L)
                .conversationId(40L)
                .senderId(1L)
                .senderName("Ada Lovelace")
                .content("Check this course")
                .sharedCourseId(20L)
                .sharedCourseTitle("Algorithms")
                .isRead(false)
                .isMine(true)
                .build());

        mockMvc.perform(post("/api/community/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conversationId", 40L,
                                "recipientId", 2L,
                                "content", "Check this course",
                                "courseIdToShare", 20L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(300))
                .andExpect(jsonPath("$.sharedCourseId").value(20))
                .andExpect(jsonPath("$.sharedCourseTitle").value("Algorithms"));
    }

    @Test
    void studySessionLifecycle_startsKeepsAliveAndStopsSession() throws Exception {
        when(studySessionService.startStudySession(1L, 20L, 101L))
                .thenReturn(StudySessionResponse.builder()
                        .sessionId(400L)
                        .active(true)
                        .totalActiveSeconds(0)
                        .build());
        when(studySessionService.keepSessionActive(1L, 400L))
                .thenReturn(StudySessionResponse.builder()
                        .sessionId(400L)
                        .active(true)
                        .totalActiveSeconds(60)
                        .build());
        when(studySessionService.stopStudySession(1L, 400L))
                .thenReturn(StudySessionResponse.builder()
                        .sessionId(400L)
                        .active(false)
                        .totalActiveSeconds(180)
                        .build());

        mockMvc.perform(post("/api/study-sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "courseId", 20L,
                                "lessonId", 101L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(400))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/study-sessions/keep-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sessionId", 400L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveSeconds").value(60));

        mockMvc.perform(post("/api/study-sessions/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sessionId", 400L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.totalActiveSeconds").value(180));

        verify(studySessionService).stopStudySession(1L, 400L);
    }
}
