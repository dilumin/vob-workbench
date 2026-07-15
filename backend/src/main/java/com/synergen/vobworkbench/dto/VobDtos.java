package com.synergen.vobworkbench.dto;

import com.synergen.vobworkbench.model.CoverageSummary;
import com.synergen.vobworkbench.model.EligibilityResult;
import com.synergen.vobworkbench.model.InsurancePolicy;
import com.synergen.vobworkbench.model.Priority;
import com.synergen.vobworkbench.model.Procedure;
import com.synergen.vobworkbench.model.VobStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class VobDtos {
    private VobDtos() {
    }

    public record VobRequestCreate(
            @NotBlank String patientId,
            @NotNull @FutureOrPresent LocalDate dateOfService,
            @NotNull Priority priority,
            String assignedTo,
            @NotEmpty List<InsurancePolicy> insurancePolicies,
            @NotEmpty List<Procedure> procedures
    ) {
    }

    public record VobRequestPatch(
            LocalDate dateOfService,
            Priority priority,
            String assignedTo,
            List<InsurancePolicy> insurancePolicies,
            List<Procedure> procedures,
            EligibilityResult eligibilityResult,
            String note,
            String reason,
            @NotNull Long version
    ) {
    }

    public record VobRequestResponse(
            String id,
            String patientId,
            LocalDate dateOfService,
            Priority priority,
            String assignedTo,
            List<InsurancePolicy> insurancePolicies,
            List<Procedure> procedures,
            VobStatus status,
            boolean locked,
            Long version,
            EligibilityResult eligibilityResult,
            CoverageSummary coverageSummary,
            List<String> notes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
