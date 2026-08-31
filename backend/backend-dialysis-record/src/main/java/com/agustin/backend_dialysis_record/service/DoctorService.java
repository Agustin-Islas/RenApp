package com.agustin.backend_dialysis_record.service;

import com.agustin.backend_dialysis_record.dto.DoctorDto;
import com.agustin.backend_dialysis_record.dto.DoctorMeDto;
import com.agustin.backend_dialysis_record.dto.PatientDto;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

public interface DoctorService {
    List<DoctorDto> findAll();

    DoctorDto findById(UUID id);

    DoctorDto create(DoctorDto doctorDto);

    DoctorDto update(UUID id, DoctorDto doctorDto);

    void delete(UUID id);

    PatientDto addPatientToDoctor(UUID doctorId, UUID patientId);

    List<PatientDto> getPatientsByDoctor(UUID doctorId);

    void removePatientFromDoctor(UUID doctorId, UUID patientId);

    DoctorDto activate(UUID doctorId);

    @Nullable DoctorMeDto getMyDoctor(UUID userAccountId);

    @Nullable List<PatientDto> getMyPatients(UUID userAccountId);

    @Nullable PatientDto addPatientToMyDoctor(UUID userAccountId, UUID patientId);

    void removePatientFromMyDoctor(UUID userAccountId, UUID patientId);
}
