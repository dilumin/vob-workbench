package com.synergen.vobworkbench.controller;

import com.synergen.vobworkbench.dto.AuthDtos.AuthResponse;
import com.synergen.vobworkbench.dto.AuthDtos.LoginRequest;
import com.synergen.vobworkbench.dto.AuthDtos.LogoutRequest;
import com.synergen.vobworkbench.dto.AuthDtos.LogoutResponse;
import com.synergen.vobworkbench.dto.AuthDtos.RefreshTokenRequest;
import com.synergen.vobworkbench.dto.AuthDtos.UserResponse;
import com.synergen.vobworkbench.security.SecurityUtils;
import com.synergen.vobworkbench.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SecurityUtils securityUtils;

    public AuthController(AuthService authService, SecurityUtils securityUtils) {
        this.authService = authService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public LogoutResponse logout(@Valid @RequestBody LogoutRequest request) {
        return authService.logout(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.currentUser(securityUtils.username());
    }
}
