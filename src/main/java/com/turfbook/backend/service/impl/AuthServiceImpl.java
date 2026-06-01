package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.UserMapper;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.JwtTokenProvider;
import com.turfbook.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        UserEntity.Role role;
        try {
            role = UserEntity.Role.valueOf(request.getRole().name().toUpperCase());
        } catch (Exception e) {
            role = UserEntity.Role.PLAYER;
        }
        // Prevent self-registration as ADMIN
        if (role == UserEntity.Role.ADMIN) {
            role = UserEntity.Role.PLAYER;
        }

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token); // In production, use a separate refresh token store
        response.setUser(userMapper.toDto(user));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserEntity user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isBlocked()) {
            throw new UnauthorizedException("Your account has been blocked. Please contact support.");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token);
        response.setUser(userMapper.toDto(user));
        return response;
    }

    @Override
    public MessageResponse sendOtp(OtpSendRequest request) {
        // TODO: Integrate with SMS provider (Twilio, AWS SNS, etc.)
        log.info("OTP send requested for phone: {}", request.getPhone());
        MessageResponse response = new MessageResponse();
        response.setMessage("OTP sent successfully to " + request.getPhone());
        return response;
    }

    @Override
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        // TODO: Verify OTP against stored code
        // For now, accept any OTP (demo mode)
        log.warn("OTP verification in demo mode — accepting any code for phone: {}", request.getPhone());

        // Try to find user by phone; create a temporary guest response if not found
        UserEntity user = userRepository.findAll().stream()
                .filter(u -> request.getPhone().equals(u.getPhone()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("No user found with phone: " + request.getPhone()));

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token);
        response.setUser(userMapper.toDto(user));
        return response;
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // TODO: Send password reset email
        log.info("Password reset requested for email: {}", request.getEmail());
        MessageResponse response = new MessageResponse();
        response.setMessage("If an account with this email exists, a password reset link has been sent.");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newToken = tokenProvider.generateToken(user.getId(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(newToken);
        response.setRefreshToken(newToken);
        response.setUser(userMapper.toDto(user));
        return response;
    }
}
