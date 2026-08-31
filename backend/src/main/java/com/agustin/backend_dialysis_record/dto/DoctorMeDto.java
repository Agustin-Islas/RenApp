package com.agustin.backend_dialysis_record.dto;

import com.agustin.backend_dialysis_record.model.auth.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorMeDto extends DoctorDto {
    private String email;
    private UserRole role;
    private int patientCount;
}
