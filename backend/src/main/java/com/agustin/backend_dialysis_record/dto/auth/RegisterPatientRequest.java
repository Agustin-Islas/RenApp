package com.agustin.backend_dialysis_record.dto.auth;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record RegisterPatientRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        // password eliminado: la identidad es gestionada por Supabase Auth

        @NotBlank
        @Size(max = 60)
        String name,

        @NotBlank
        @Size(max = 60)
        String surname,

        @NotNull
        @Min(1_000_000)
        @Max(99_999_999)
        Integer dni,

        @NotNull
        @Past
        LocalDate dateOfBirth,

        @NotBlank
        @Size(max = 120)
        String address,

        @Min(1)
        Long number
) {}
