package com.agustin.backend_dialysis_record.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInvitationDto {
    private Integer patientDni;
    private String patientEmail;
}
