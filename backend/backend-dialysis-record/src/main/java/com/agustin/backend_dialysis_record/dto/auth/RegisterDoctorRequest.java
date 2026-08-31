package com.agustin.backend_dialysis_record.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDoctorRequest(
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
        String surname
) {}