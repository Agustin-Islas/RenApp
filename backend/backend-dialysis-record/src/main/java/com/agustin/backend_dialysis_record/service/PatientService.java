package com.agustin.backend_dialysis_record.service;

import com.agustin.backend_dialysis_record.dto.PatientDto;
import com.agustin.backend_dialysis_record.dto.PatientMeDto;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

public interface PatientService {
    List<PatientDto> findAll();
    List<PatientDto> findPatientsByDoctor(UUID doctorId);

    PatientDto findById(UUID id);

    PatientDto create(PatientDto patientDto, UUID creatorDoctorId);

    PatientDto update(UUID id, PatientDto patientDto);

    void delete(UUID id);

    PatientDto activate(UUID patientId);

    @Nullable
    PatientMeDto getMyPatient(UUID userAccountId);
}
