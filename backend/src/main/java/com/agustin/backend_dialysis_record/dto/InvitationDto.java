package com.agustin.backend_dialysis_record.dto;

import com.agustin.backend_dialysis_record.model.InvitationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class InvitationDto {
    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private Integer patientDni;
    private String patientEmail;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
