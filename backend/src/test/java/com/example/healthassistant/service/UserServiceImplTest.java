package com.example.healthassistant.service;

import com.example.healthassistant.dto.LoginRequestDto;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.repository.DietRecordRepository;
import com.example.healthassistant.repository.FitnessRecordRepository;
import com.example.healthassistant.repository.UserProfileRepository;
import com.example.healthassistant.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private DietRecordRepository dietRecordRepository;

    @Mock
    private FitnessRecordRepository fitnessRecordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserProfile existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new UserProfile();
        existingUser.setUserId("testuser");
        existingUser.setPassword("encoded");
    }

    @Test
    void login_success_whenPasswordMatches() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("123456");

        when(userProfileRepository.findByUserId("testuser")).thenReturn(existingUser);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        UserProfile result = userService.login(request);

        assertNotNull(result);
        assertEquals("testuser", result.getUserId());
    }

    @Test
    void login_fails_whenPasswordWrong() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("wrong");

        when(userProfileRepository.findByUserId("testuser")).thenReturn(existingUser);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertNull(userService.login(request));
    }

    @Test
    void register_fails_whenUsernameExists() {
        when(userProfileRepository.findByUserId("testuser")).thenReturn(existingUser);

        assertNull(userService.register("testuser", "123456"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateUsername_cascadesToRelatedRecords() {
        when(userProfileRepository.findByUserId("newname")).thenReturn(null);
        when(userProfileRepository.findByUserId("testuser")).thenReturn(existingUser);

        boolean updated = userService.updateUsername("testuser", "newname");

        assertTrue(updated);
        verify(dietRecordRepository).updateUserId("testuser", "newname");
        verify(fitnessRecordRepository).updateUserId("testuser", "newname");
        verify(userProfileRepository).save(existingUser);
        assertEquals("newname", existingUser.getUserId());
    }
}
