package com.agustin.backend_dialysis_record.mapper;

import com.agustin.backend_dialysis_record.dto.PatientDto;
import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.repository.DoctorRepository;
import com.agustin.backend_dialysis_record.repository.SessionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PatientMapper implements GenericMapper<Patient, PatientDto> {
    private final SessionRepository sessionRepository;
    private final DoctorRepository doctorRepository;

    public PatientMapper(SessionRepository sessionRepository, DoctorRepository doctorRepository) {
        this.sessionRepository = sessionRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    public Patient toEntity(PatientDto patientDto) {
        if (patientDto == null) { return null; }

        Patient patient = new Patient();
        patient.setName(patientDto.getName());
        patient.setSurname(patientDto.getSurname());
        patient.setDni(patientDto.getDni());
        patient.setDateOfBirth(patientDto.getDateOfBirth());
        patient.setAddress(patientDto.getAddress());
        patient.setNumber(patientDto.getNumber());
        patient.setCustomConcentrations(copyCustomConcentrations(patientDto));
        return patient;
    }

    @Override
    public PatientDto toDto(Patient patient) {
        if  (patient == null) { return null; }

        PatientDto patientDto = new PatientDto();
        patientDto.setId(patient.getId());
        patientDto.setName(patient.getName());
        patientDto.setSurname(patient.getSurname());
        patientDto.setDni(patient.getDni());
        patientDto.setDateOfBirth(patient.getDateOfBirth());
        patientDto.setAddress(patient.getAddress());
        patientDto.setNumber(patient.getNumber());
        patientDto.setCustomConcentrations(new ArrayList<>(patient.getCustomConcentrations()));
        return patientDto;
    }

    @Override
    public void updateEntityFromDTO(Patient patient, PatientDto patientDto) {
        if (patient == null || patientDto == null) { return; }

        patient.setName(patientDto.getName());
        patient.setSurname(patientDto.getSurname());
        patient.setDni(patientDto.getDni());
        patient.setDateOfBirth(patientDto.getDateOfBirth());
        patient.setAddress(patientDto.getAddress());
        patient.setNumber(patientDto.getNumber());
        
        if (patient.getCustomConcentrations() == null) {
            patient.setCustomConcentrations(new ArrayList<>());
        }
        patient.getCustomConcentrations().clear();
        if (patientDto.getCustomConcentrations() != null) {
            patient.getCustomConcentrations().addAll(patientDto.getCustomConcentrations());
        }
        
    }


    private ArrayList<Float> copyCustomConcentrations(PatientDto patientDto) {
        return new ArrayList<>(patientDto.getCustomConcentrations() == null ? Collections.emptyList() : patientDto.getCustomConcentrations());
    }
}
