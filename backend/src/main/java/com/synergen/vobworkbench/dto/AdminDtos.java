package com.synergen.vobworkbench.dto;

import com.synergen.vobworkbench.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record UserCreate(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String fullName,
            @NotNull Role role
    ) {
    }

    public record DashboardSummary(
            long totalRequests,
            long pending,
            long inProgress,
            long verified,
            long urgent
    ) {
    }
}
