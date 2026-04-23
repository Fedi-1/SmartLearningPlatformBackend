// src/main/java/com/example/SmartLearningPlatformBackend/service/CommunityService.java
package com.example.SmartLearningPlatformBackend.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CourseShareInviteRepository courseShareInviteRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final DocumentRepository documentRepository;
    private final SseService sseService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ConversationDTO> getMyConversations(Long studentId) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
        }

        return conversationRepository.findAllForStudentOrderByLastMessageAtDesc(studentId)
                .stream()
                .map(conversation -> toConversationDTO(conversation, studentId))
                .toList();
    }

    @Transactional
    public ConversationDTO getOrCreateConversation(Long studentId, Long otherStudentId) {
        if (studentId == null || otherStudentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId and otherStudentId are required");
        }
        if (studentId.equals(otherStudentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create conversation with yourself");
        }

        requireUser(studentId);
        requireUser(otherStudentId);

        Long studentOneId = Math.min(studentId, otherStudentId);
        Long studentTwoId = Math.max(studentId, otherStudentId);

        Conversation conversation = conversationRepository
                .findByStudentOneIdAndStudentTwoId(studentOneId, studentTwoId)
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .studentOneId(studentOneId)
                        .studentTwoId(studentTwoId)
                        .build()));

        return toConversationDTO(conversation, studentId);
    }

    @Transactional
    public List<MessageDTO> getMessages(Long conversationId, Long studentId) {
        if (conversationId == null || studentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId and studentId are required");
        }

        requireConversationForParticipant(conversationId, studentId);

        messageRepository.markAllAsReadForRecipient(conversationId, studentId);

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(message -> toMessageDTO(message, studentId))
                .toList();
    }

    @Transactional
    public MessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        if (senderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senderId is required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
        }

        Long conversationId = request.getConversationId();
        Long requestRecipientId = request.getRecipientId();

        if (conversationId == null) {
            if (requestRecipientId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "recipientId is required when conversationId is null");
            }
            ConversationDTO conversationDTO = getOrCreateConversation(senderId, requestRecipientId);
            conversationId = conversationDTO.getId();
        }

        final Long finalConversationId = conversationId;

        Conversation conversation = conversationRepository.findById(finalConversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation not found: " + finalConversationId));

        if (!isParticipant(conversation, senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        Long recipientId = getOtherParticipantId(conversation, senderId);
        if (requestRecipientId != null && !requestRecipientId.equals(recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "recipientId does not match conversation participant");
        }

        User sender = requireUser(senderId);
        String senderName = buildFullName(sender);

        Long sharedCourseId = null;
        if (request.getCourseIdToShare() != null) {
            Course originalCourse = courseRepository.findById(request.getCourseIdToShare())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Course not found: " + request.getCourseIdToShare()));

            if (!senderId.equals(originalCourse.getStudentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share your own courses.");
            }

            Document originalDocument = documentRepository.findById(originalCourse.getDocumentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Document not found for course: " + originalCourse.getId()));

            Course clonedCourse = courseService.cloneCourseForStudent(originalCourse, originalDocument, recipientId);

            courseShareInviteRepository.save(CourseShareInvite.builder()
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .originalCourseId(originalCourse.getId())
                    .clonedCourseId(clonedCourse.getId())
                    .status("ACCEPTED")
                    .respondedAt(LocalDateTime.now())
                    .build());

            sharedCourseId = clonedCourse.getId();

            notificationService.notify(
                    recipientId,
                    NotificationCategory.COURSE_COMPLETE,
                    "Course Shared With You 🎓",
                    senderName + " shared \"" + originalCourse.getTitle()
                            + "\" with you. It has been added to your courses.",
                    clonedCourse.getId(),
                    "/dashboard/courses/" + clonedCourse.getId());
        }

        Message savedMessage = messageRepository.save(Message.builder()
                .conversationId(finalConversationId)
                .senderId(senderId)
                .content(request.getContent().trim())
                .sharedCourseId(sharedCourseId)
                .build());

        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        sseService.sendToUser(recipientId, Map.of(
                "type", "NEW_MESSAGE",
                "conversationId", finalConversationId,
                "senderName", senderName,
                "preview", firstChars(savedMessage.getContent(), 50)));

        return toMessageDTO(savedMessage, senderId);
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResult> searchStudents(String query, Long currentStudentId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        if (currentStudentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currentStudentId is required");
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByRole(UserRole.STUDENT)
                .stream()
                .filter(user -> !user.getId().equals(currentStudentId))
                .filter(user -> {
                    String fullName = buildFullName(user).toLowerCase(Locale.ROOT);
                    String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase(Locale.ROOT);
                    return fullName.contains(normalized) || email.contains(normalized);
                })
                .limit(10)
                .map(user -> StudentSearchResult.builder()
                        .id(user.getId())
                        .fullName(buildFullName(user))
                        .initials(buildInitials(user.getFirstName(), user.getLastName()))
                        .email(user.getEmail())
                        .build())
                .toList();
    }

    private ConversationDTO toConversationDTO(Conversation conversation, Long studentId) {
        Long otherStudentId = getOtherParticipantId(conversation, studentId);
        User otherStudent = requireUser(otherStudentId);

        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId());
        Message lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);

        return ConversationDTO.builder()
                .id(conversation.getId())
                .otherStudentId(otherStudentId)
                .otherStudentName(buildFullName(otherStudent))
                .otherStudentInitials(buildInitials(otherStudent.getFirstName(), otherStudent.getLastName()))
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(
                        conversation.getId(), studentId))
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private MessageDTO toMessageDTO(Message message, Long currentStudentId) {
        User sender = requireUser(message.getSenderId());

        String sharedCourseTitle = null;
        if (message.getSharedCourseId() != null) {
            sharedCourseTitle = courseRepository.findById(message.getSharedCourseId())
                    .map(Course::getTitle)
                    .orElse(null);
        }

        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderName(buildFullName(sender))
                .isRead(message.getIsRead())
                .sentAt(message.getSentAt())
                .content(message.getContent())
                .sharedCourseId(message.getSharedCourseId())
                .sharedCourseTitle(sharedCourseTitle)
                .isMine(message.getSenderId().equals(currentStudentId))
                .build();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private void requireConversationForParticipant(Long conversationId, Long studentId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation not found: " + conversationId));

        if (!isParticipant(conversation, studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
    }

    private boolean isParticipant(Conversation conversation, Long userId) {
        return conversation.getStudentOneId().equals(userId) || conversation.getStudentTwoId().equals(userId);
    }

    private Long getOtherParticipantId(Conversation conversation, Long studentId) {
        if (conversation.getStudentOneId().equals(studentId)) {
            return conversation.getStudentTwoId();
        }
        if (conversation.getStudentTwoId().equals(studentId)) {
            return conversation.getStudentOneId();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
    }

    private String buildFullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Unknown User" : full;
    }

    private String buildInitials(String firstName, String lastName) {
        String first = (firstName == null || firstName.isBlank()) ? "" : firstName.trim().substring(0, 1);
        String last = (lastName == null || lastName.isBlank()) ? "" : lastName.trim().substring(0, 1);
        String initials = (first + last).toUpperCase(Locale.ROOT);
        return initials.isBlank() ? "?" : initials;
    }

    private String firstChars(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
