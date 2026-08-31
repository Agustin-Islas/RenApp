package com.agustin.backend_dialysis_record.mapper;

import com.agustin.backend_dialysis_record.dto.DoctorDto;
import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DoctorMapper implements GenericMapper<Doctor, DoctorDto> {

    private final PatientRepository patientRepository;

    public DoctorMapper(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Doctor toEntity(DoctorDto doctorDto) {
        if (doctorDto == null) { return null; }

        Doctor doctor = new Doctor();
        doctor.setName(doctorDto.getName());
        doctor.setSurname(doctorDto.getSurname());

        return doctor;
    }

    @Override
    public DoctorDto toDto(Doctor doctor) {
        if (doctor == null) { return null; }

        DoctorDto doctorDto = new DoctorDto();
        doctorDto.setId(doctor.getId());
        doctorDto.setName(doctor.getName());
        doctorDto.setSurname(doctor.getSurname());

        return doctorDto;
    }

    @Override
    public void updateEntityFromDTO(Doctor doctor, DoctorDto doctorDto) {
        if (doctor == null || doctorDto == null) {
            return; //Todo: exception?
        }
        doctor.setName(doctorDto.getName());
        doctor.setSurname(doctorDto.getSurname());

    }
}
