package com.synergen.vobworkbench.service;

import com.synergen.vobworkbench.dto.AuthDtos.AuthResponse;
import com.synergen.vobworkbench.dto.AuthDtos.LoginRequest;
import com.synergen.vobworkbench.dto.AuthDtos.LogoutRequest;
import com.synergen.vobworkbench.dto.AuthDtos.LogoutResponse;
import com.synergen.vobworkbench.dto.AuthDtos.RefreshTokenRequest;
import com.synergen.vobworkbench.dto.AuthDtos.UserResponse;
import com.synergen.vobworkbench.exception.BusinessException;
import com.synergen.vobworkbench.model.RefreshToken;
import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.repository.UserRepository;
import com.synergen.vobworkbench.security.JwtService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final VobRequestService vobRequestService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager,
                       JwtService jwtService, RefreshTokenService refreshTokenService,
                       VobRequestService vobRequestService, AuditService auditService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.vobRequestService = vobRequestService;
        this.auditService = auditService;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            auditService.logLoginFailed(request.username());
            throw invalidCredentials();
        }

        User user = userRepository.findByUsername(request.username())
                .filter(User::isActive)
                .orElseThrow(() -> {
                    auditService.logLoginFailed(request.username());
                    return invalidCredentials();
                });
        String refreshToken = refreshTokenService.createFor(user);
        auditService.logLoginSucceeded(user);
        dispatchQueuedWorkIfSpecialist(user);
        return authResponse(user, refreshToken);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken oldToken = refreshTokenService.validate(request.refreshToken());
        User user = userRepository.findByUsername(oldToken.getUsername())
                .filter(User::isActive)
                .orElseThrow(this::invalidCredentials);

        refreshTokenService.revoke(oldToken);
        String refreshToken = refreshTokenService.createFor(user);
        auditService.logTokenRefreshed(user);
        dispatchQueuedWorkIfSpecialist(user);
        return authResponse(user, refreshToken);
    }

    public LogoutResponse logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        auditService.logLogoutSucceeded();
        return new LogoutResponse("Logged out successfully.");
    }

    public UserResponse currentUser(String username) {
        return userRepository.findByUsername(username)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Current user was not found."));
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole(), user.isActive());
    }

    private AuthResponse authResponse(User user, String refreshToken) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                user.getUsername(),
                List.of(user.getRole().name())
        );
    }

    private void dispatchQueuedWorkIfSpecialist(User user) {
        if (user.getRole() == Role.SPECIALIST) {
            vobRequestService.dispatchNextQueuedRequest();
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid username or password.");
    }
}
