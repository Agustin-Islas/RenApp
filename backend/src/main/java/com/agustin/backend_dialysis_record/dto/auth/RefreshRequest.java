package com.agustin.backend_dialysis_record.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RefreshRequest {
    @NotBlank
    private String refreshToken;
}
