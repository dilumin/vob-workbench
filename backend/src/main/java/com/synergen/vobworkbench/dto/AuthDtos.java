package com.synergen.vobworkbench.dto;

import com.synergen.vobworkbench.model.Role;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String username,
            List<String> roles
    ) {
    }

    public record LogoutResponse(String message) {
    }

    public record UserResponse(String id, String username, String fullName, Role role, boolean active) {
    }
}
