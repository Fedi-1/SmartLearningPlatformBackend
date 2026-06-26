package com.example.SmartLearningPlatformBackend.integration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SmartLearningPlatformBackend.controller.AdminController;
import com.example.SmartLearningPlatformBackend.controller.CertificateController;
import com.example.SmartLearningPlatformBackend.controller.NotificationController;
import com.example.SmartLearningPlatformBackend.dto.admin.AdminCertificateItem;
import com.example.SmartLearningPlatformBackend.dto.admin.AdminContentItem;
import com.example.SmartLearningPlatformBackend.dto.admin.AdminStatsResponse;
import com.example.SmartLearningPlatformBackend.dto.admin.StudentSummaryResponse;
import com.example.SmartLearningPlatformBackend.dto.notification.NotificationDTO;
import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.enums.NotificationType;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.service.AdminService;
import com.example.SmartLearningPlatformBackend.service.CertificateService;
import com.example.SmartLearningPlatformBackend.service.NotificationService;
import com.example.SmartLearningPlatformBackend.service.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class Sprint3AdministrationIntegrationTest {

    @Mock
    private AdminService adminService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SseService sseService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private CertificateRepository certificateRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@mail.test")
                .password("password")
                .role(UserRole.ADMIN)
                .build();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new AdminController(adminService),
                        new NotificationController(notificationService, sseService),
                        new CertificateController(certificateService, certificateRepository))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(new UserDetailsImpl(admin)))
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void adminStudentManagement_listsStudentsAndTogglesAccountStatus() throws Exception {
        when(adminService.getAllStudents()).thenReturn(List.of(StudentSummaryResponse.builder()
                .id(2L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@mail.test")
                .isActive(true)
                .coursesCount(3)
                .engagementScore(85)
                .engagementLevel("HIGH")
                .build()));
        when(adminService.toggleStudentStatus(2L)).thenReturn(false);

        mockMvc.perform(get("/api/admin/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].email").value("ada@mail.test"))
                .andExpect(jsonPath("$[0].isActive").value(true));

        mockMvc.perform(patch("/api/admin/students/2/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void adminContentMonitoring_returnsContentItemsAndPlatformStats() throws Exception {
        when(adminService.getContentItems()).thenReturn(List.of(AdminContentItem.builder()
                .documentId(10L)
                .documentFileName("lesson.pdf")
                .documentFileType("PDF")
                .documentStatus("COMPLETED")
                .studentId(2L)
                .studentName("Ada Lovelace")
                .studentEmail("ada@mail.test")
                .courseId(20L)
                .courseTitle("Algorithms")
                .totalLessons(5)
                .build()));
        when(adminService.getStats()).thenReturn(AdminStatsResponse.builder()
                .totalStudents(12)
                .totalCourses(18)
                .totalCertificates(4)
                .totalDocuments(30)
                .examPassRate(76)
                .build());

        mockMvc.perform(get("/api/admin/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(10))
                .andExpect(jsonPath("$[0].courseTitle").value("Algorithms"));

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(12))
                .andExpect(jsonPath("$.examPassRate").value(76));
    }

    @Test
    void notifications_returnsHistoryUnreadCountAndMarksNotificationRead() throws Exception {
        NotificationDTO unreadNotification = NotificationDTO.builder()
                .id(100L)
                .userId(1L)
                .type(NotificationType.IN_APP)
                .category(NotificationCategory.CERTIFICATE)
                .title("Certificate Approved")
                .message("Your certificate is ready.")
                .isRead(false)
                .build();
        NotificationDTO readNotification = NotificationDTO.builder()
                .id(100L)
                .userId(1L)
                .type(NotificationType.IN_APP)
                .category(NotificationCategory.CERTIFICATE)
                .title("Certificate Approved")
                .message("Your certificate is ready.")
                .isRead(true)
                .build();

        when(notificationService.getUserNotifications(1L)).thenReturn(List.of(unreadNotification));
        when(notificationService.getUnreadCount(1L)).thenReturn(1L);
        when(notificationService.markAsRead(100L)).thenReturn(readNotification);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Certificate Approved"))
                .andExpect(jsonPath("$[0].isRead").value(false));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(put("/api/notifications/100/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void adminCertificateManagement_listsApprovesAndRevokesCertificates() throws Exception {
        when(adminService.getAllCertificates()).thenReturn(List.of(AdminCertificateItem.builder()
                .id(200L)
                .certificateUUID("cert-uuid")
                .studentName("Ada Lovelace")
                .studentEmail("ada@mail.test")
                .courseTitle("Algorithms")
                .score(92)
                .status(CertificateStatus.PENDING)
                .build()));

        mockMvc.perform(get("/api/admin/certificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(patch("/api/admin/certificates/200/approve"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/certificates/200/revoke"))
                .andExpect(status().isOk());

        verify(adminService).approveCertificate(200L);
        verify(adminService).revokeCertificate(200L);
    }

    @Test
    void approvedCertificateDownload_returnsPdfBytesByPublicUuid() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 certificate".getBytes();
        when(certificateRepository.findByCertificateUuid("cert-uuid")).thenReturn(Optional.of(Certificate.builder()
                .id(200L)
                .certificateUuid("cert-uuid")
                .studentId(2L)
                .courseId(20L)
                .examAttemptId(90L)
                .score(92)
                .status(CertificateStatus.APPROVED)
                .pdfContent(pdfBytes)
                .build()));

        mockMvc.perform(get("/api/certificates/cert-uuid/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"certificate_cert-uuid.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }
}
