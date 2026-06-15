package com.serviceonwheels.auth_service.service;

import com.serviceonwheels.auth_service.dto.RegisterRequest;
import com.serviceonwheels.auth_service.model.Role;
import com.serviceonwheels.auth_service.model.User;
import com.serviceonwheels.auth_service.repository.UserRepository;
import com.serviceonwheels.auth_service.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthService authService;

    @Test
    void selfRegistrationAlwaysCreatesCustomerRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Customer");
        request.setEmail("customer@example.com");
        request.setPassword("Password123!");
        request.setPhoneNumber("9876543210");

        when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(userDetailsService.loadUserByUsername("customer@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails, "USER")).thenReturn("token");

        authService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.USER, userCaptor.getValue().getRole());
    }
}
