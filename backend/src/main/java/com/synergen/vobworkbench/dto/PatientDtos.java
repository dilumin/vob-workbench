package com.synergen.vobworkbench.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import java.time.Instant;
import java.time.LocalDate;

public final class PatientDtos {
    private PatientDtos() {
    }

    public record PatientWrite(
            @NotBlank String mrn,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Past LocalDate dateOfBirth,
            String gender,
            String phone
    ) {
    }

    public record PatientPatch(
            String firstName,
            String lastName,
            @Past LocalDate dateOfBirth,
            String gender,
            String phone
    ) {
    }

    public record PatientResponse(
            String id,
            String mrn,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String gender,
            String phone,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
