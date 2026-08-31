package com.agustin.backend_dialysis_record.service.impl;

import com.agustin.backend_dialysis_record.dto.PatientDto;
import com.agustin.backend_dialysis_record.dto.PatientMeDto;
import com.agustin.backend_dialysis_record.mapper.PatientMapper;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.Session;
import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import com.agustin.backend_dialysis_record.repository.DoctorPatientAccessRepository;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {
    private static final Set<Float> FIXED_CONCENTRATIONS = Set.of(1.5f, 2.3f, 3.8f);

    private final PatientRepository patientRepository;
    private final UserAccountRepository userAccountRepository;
    private final DoctorPatientAccessRepository doctorPatientAccessRepository;
    private final PatientMapper patientMapper;
    private final SessionServiceImpl sessionService;

    @Autowired
    public PatientServiceImpl(PatientRepository patientRepository, UserAccountRepository userAccountRepository, DoctorPatientAccessRepository doctorPatientAccessRepository, PatientMapper patientMapper, SessionServiceImpl sessionService) {
        this.patientRepository = patientRepository;
        this.userAccountRepository = userAccountRepository;
        this.doctorPatientAccessRepository = doctorPatientAccessRepository;
        this.patientMapper = patientMapper;
        this.sessionService = sessionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> findAll() {
        return patientRepository.findAll()
                .stream().map(patientMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> findPatientsByDoctor(UUID doctorId) {
        return doctorPatientAccessRepository.findByDoctorId(doctorId)
                .stream()
                .map(access -> patientMapper.toDto(access.getPatient()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDto findById(UUID id) {
        return patientRepository.findById(id).map(patientMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
    }

    @Override
    public PatientDto create(PatientDto patientDto, UUID creatorDoctorId) {
        validateCustomConcentrations(patientDto.getCustomConcentrations());
        Patient savePatient = patientRepository.save(patientMapper.toEntity(patientDto));
        
        if (creatorDoctorId != null) {
            com.agustin.backend_dialysis_record.model.Doctor doctor = new com.agustin.backend_dialysis_record.model.Doctor();
            doctor.setId(creatorDoctorId);
            com.agustin.backend_dialysis_record.model.DoctorPatientAccess access = new com.agustin.backend_dialysis_record.model.DoctorPatientAccess();
            access.setDoctor(doctor);
            access.setPatient(savePatient);
            doctorPatientAccessRepository.save(access);
        }
        
        return patientMapper.toDto(savePatient);
    }

    @Override
    public PatientDto update(UUID id, PatientDto patientDto) {
        if (patientDto.getId() != null && !patientDto.getId().equals(id))
            throw new IllegalArgumentException("Path id and DTO id must match");

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
        validateCustomConcentrations(patientDto.getCustomConcentrations());
        patientMapper.updateEntityFromDTO(patient, patientDto);
        Patient savePatient = patientRepository.save(patient);
        return patientMapper.toDto(savePatient);
    }

    @Override
    public void delete(UUID id) {
        if (!patientRepository.existsById(id))
            throw new RuntimeException("Patient not found with id: " + id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        List<Session> sessions = patient.getSessions();
        sessions.forEach(session -> sessionService.delete(session.getId()));

        patientRepository.deleteById(id);
    }

    @Override
    public PatientDto activate(UUID patientId) {
        Patient patient = patientRepository.findByIdIncludingInactive(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));
        patient.setActive(true);
        return patientMapper.toDto(patientRepository.save(patient));
    }

    @Override
    public PatientMeDto getMyPatient(UUID authId) {
        UserAccount ua = userAccountRepository.findByAuthId(authId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Account non exist"));

        UUID patientId = userAccountRepository.findPatientIdByUserAccountId(ua.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Patient not linked to this account"));


        PatientDto base = findById(patientId); // <- devuelve PatientDto

        PatientMeDto dto = new PatientMeDto();

        //by patient
        dto.setId(base.getId());
        dto.setName(base.getName());
        dto.setSurname(base.getSurname());
        dto.setDni(base.getDni());
        dto.setDateOfBirth(base.getDateOfBirth());
        dto.setAddress(base.getAddress());
        dto.setNumber(base.getNumber());
        dto.setCustomConcentrations(base.getCustomConcentrations());

        //by user
        dto.setEmail(ua.getEmail());
        dto.setRole(ua.getRole());

        //by access
        var accesses = doctorPatientAccessRepository.findByPatientId(patientId);
        if (!accesses.isEmpty()) {
            String names = accesses.stream()
                .map(a -> a.getDoctor().getName() + " " + a.getDoctor().getSurname())
                .collect(java.util.stream.Collectors.joining(", "));
            dto.setDoctorName(names);
        }

        return dto;
    }

    private void validateCustomConcentrations(List<Float> values) {
        if (values == null || values.isEmpty()) return;

        Set<Integer> normalized = new HashSet<>();
        for (Float value : values) {
            if (value == null) {
                throw new IllegalArgumentException("custom concentration cannot be null");
            }
            if (value < 0.1f || value > 10.0f) {
                throw new IllegalArgumentException("custom concentrations must be between 0.1 and 10.0");
            }
            int key = Math.round(value * 10);
            if (Math.abs(value * 10 - key) > 0.0001f) {
                throw new IllegalArgumentException("custom concentrations must use one decimal");
            }
            if (FIXED_CONCENTRATIONS.stream().anyMatch(fixed -> sameConcentration(fixed, value))) {
                throw new IllegalArgumentException("custom concentrations cannot duplicate fixed concentrations");
            }
            if (!normalized.add(key)) {
                throw new IllegalArgumentException("custom concentrations cannot contain duplicates");
            }
        }
    }

    private boolean sameConcentration(float a, float b) {
        return Math.abs(a - b) < 0.0001f;
    }
}
