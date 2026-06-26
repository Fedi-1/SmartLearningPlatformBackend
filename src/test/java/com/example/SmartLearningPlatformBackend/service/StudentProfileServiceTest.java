package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.student.ChangePasswordRequest;
import com.example.SmartLearningPlatformBackend.dto.student.StudentProfileResponse;
import com.example.SmartLearningPlatformBackend.dto.student.UpdateProfileRequest;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentProfileService studentProfileService;

    @Test
    void getProfile_returnsStudentProfile() {
        Student student = student();
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        StudentProfileResponse response = studentProfileService.getProfile(1L);

        assertEquals("Ada", response.getFirstName());
        assertEquals("ada@example.com", response.getEmail());
    }

    @Test
    void updateProfile_ignoresBlankNamesAndUpdatesProvidedFields() {
        Student student = student();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName(" ");
        request.setLastName("Byron");
        request.setPhoneNumber("555-0100");
        request.setDateOfBirth(LocalDate.of(1815, 12, 10));

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        StudentProfileResponse response = studentProfileService.updateProfile(1L, request);

        assertEquals("Ada", response.getFirstName());
        assertEquals("Byron", response.getLastName());
        assertEquals("555-0100", response.getPhoneNumber());
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        Student student = student();
        student.setPassword("old-hash");
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("new-password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> studentProfileService.changePassword(1L, request));
    }

    @Test
    void changePassword_savesEncodedNewPassword() {
        Student student = student();
        student.setPassword("old-hash");
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current");
        request.setNewPassword("new-password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        studentProfileService.changePassword(1L, request);

        assertEquals("new-hash", student.getPassword());
        verify(userRepository).save(student);
    }

    private Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setFirstName("Ada");
        student.setLastName("Lovelace");
        student.setEmail("ada@example.com");
        student.setPhoneNumber("123");
        return student;
    }
}
