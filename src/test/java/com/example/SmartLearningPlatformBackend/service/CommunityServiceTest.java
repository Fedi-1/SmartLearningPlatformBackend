package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.community.ConversationDTO;
import com.example.SmartLearningPlatformBackend.dto.community.MessageDTO;
import com.example.SmartLearningPlatformBackend.dto.community.SendMessageRequest;
import com.example.SmartLearningPlatformBackend.dto.community.StudentSearchResult;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Conversation;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.CourseShareInvite;
import com.example.SmartLearningPlatformBackend.models.Document;
import com.example.SmartLearningPlatformBackend.models.Message;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.repository.ConversationRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseShareInviteRepository;
import com.example.SmartLearningPlatformBackend.repository.DocumentRepository;
import com.example.SmartLearningPlatformBackend.repository.MessageRepository;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private CourseShareInviteRepository courseShareInviteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private SseService sseService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommunityService communityService;

    @Test
    void getOrCreateConversation_createsConversationWithOrderedStudentIds() {
        User currentStudent = user(5L, "Ada", "Lovelace", "ada@mail.test");
        User otherStudent = user(2L, "Grace", "Hopper", "grace@mail.test");
        Conversation savedConversation = Conversation.builder()
                .id(10L)
                .studentOneId(2L)
                .studentTwoId(5L)
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(currentStudent));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherStudent));
        when(conversationRepository.findByStudentOneIdAndStudentTwoId(2L, 5L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);
        when(messageRepository.findByConversationIdOrderBySentAtAsc(10L)).thenReturn(List.of());
        when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(10L, 5L)).thenReturn(0);

        ConversationDTO result = communityService.getOrCreateConversation(5L, 2L);

        assertEquals(10L, result.getId());
        assertEquals(2L, result.getOtherStudentId());
        assertEquals("Grace Hopper", result.getOtherStudentName());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void getOrCreateConversation_rejectsSelfConversation() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> communityService.getOrCreateConversation(5L, 5L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getMessages_marksMessagesReadForCurrentStudent() {
        Conversation conversation = Conversation.builder()
                .id(20L)
                .studentOneId(1L)
                .studentTwoId(2L)
                .build();
        Message message = Message.builder()
                .id(30L)
                .conversationId(20L)
                .senderId(2L)
                .content("Hello")
                .isRead(false)
                .build();

        when(conversationRepository.findById(20L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(20L)).thenReturn(List.of(message));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Grace", "Hopper", "grace@mail.test")));

        List<MessageDTO> result = communityService.getMessages(20L, 1L);

        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0).getContent());
        verify(messageRepository).markAllAsReadForRecipient(20L, 1L);
    }

    @Test
    void sendMessage_trimsContentSavesMessageAndPushesSse() {
        Conversation conversation = Conversation.builder()
                .id(20L)
                .studentOneId(1L)
                .studentTwoId(2L)
                .build();
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(20L);
        request.setRecipientId(2L);
        request.setContent("  Great work  ");

        when(conversationRepository.findById(20L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Ada", "Lovelace", "ada@mail.test")));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(40L);
            return message;
        });

        MessageDTO result = communityService.sendMessage(1L, request);

        assertEquals("Great work", result.getContent());
        verify(conversationRepository).save(conversation);
        verify(sseService).sendToUser(eq(2L), anyMap());
    }

    @Test
    void sendMessage_rejectsSharingCourseOwnedByAnotherStudent() {
        Conversation conversation = Conversation.builder()
                .id(20L)
                .studentOneId(1L)
                .studentTwoId(2L)
                .build();
        Course course = Course.builder()
                .id(50L)
                .studentId(99L)
                .documentId(60L)
                .title("Private Course")
                .build();
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(20L);
        request.setRecipientId(2L);
        request.setContent("try this");
        request.setCourseIdToShare(50L);

        when(conversationRepository.findById(20L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Ada", "Lovelace", "ada@mail.test")));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> communityService.sendMessage(1L, request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_clonesSharedCourseAndNotifiesRecipient() {
        Conversation conversation = Conversation.builder()
                .id(20L)
                .studentOneId(1L)
                .studentTwoId(2L)
                .build();
        Course originalCourse = Course.builder()
                .id(50L)
                .studentId(1L)
                .documentId(60L)
                .title("Algorithms")
                .build();
        Course clonedCourse = Course.builder()
                .id(70L)
                .studentId(2L)
                .documentId(61L)
                .title("Algorithms")
                .build();
        Document document = Document.builder()
                .id(60L)
                .studentId(1L)
                .fileName("algorithms.pdf")
                .build();
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(20L);
        request.setRecipientId(2L);
        request.setContent("sharing this course");
        request.setCourseIdToShare(50L);

        when(conversationRepository.findById(20L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Ada", "Lovelace", "ada@mail.test")));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(originalCourse));
        when(documentRepository.findById(60L)).thenReturn(Optional.of(document));
        when(courseService.cloneCourseForStudent(originalCourse, document, 2L)).thenReturn(clonedCourse);
        when(courseRepository.findById(70L)).thenReturn(Optional.of(clonedCourse));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(40L);
            return message;
        });

        MessageDTO result = communityService.sendMessage(1L, request);

        assertEquals(70L, result.getSharedCourseId());
        assertEquals("Algorithms", result.getSharedCourseTitle());
        verify(courseShareInviteRepository).save(any(CourseShareInvite.class));
        verify(notificationService).notify(eq(2L), eq(NotificationCategory.COURSE_COMPLETE), any(), any(), eq(70L),
                eq("/dashboard/courses/70"));
    }

    @Test
    void searchStudents_filtersByNameAndExcludesCurrentStudent() {
        when(userRepository.findByRole(UserRole.STUDENT)).thenReturn(List.of(
                user(1L, "Ada", "Lovelace", "ada@mail.test"),
                user(2L, "Grace", "Hopper", "grace@mail.test"),
                user(3L, "Alan", "Turing", "alan@mail.test")));

        List<StudentSearchResult> result = communityService.searchStudents("gra", 1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals("Grace Hopper", result.get(0).getFullName());
    }

    @Test
    void searchStudents_returnsEmptyListForBlankQuery() {
        List<StudentSearchResult> result = communityService.searchStudents("   ", 1L);

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByRole(any());
    }

    private User user(Long id, String firstName, String lastName, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(UserRole.STUDENT);
        return user;
    }
}
