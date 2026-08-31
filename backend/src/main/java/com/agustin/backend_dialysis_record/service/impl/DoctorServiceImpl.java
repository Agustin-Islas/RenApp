package com.agustin.backend_dialysis_record.service.impl;

import com.agustin.backend_dialysis_record.dto.DoctorDto;
import com.agustin.backend_dialysis_record.dto.DoctorMeDto;
import com.agustin.backend_dialysis_record.dto.PatientDto;
import com.agustin.backend_dialysis_record.mapper.DoctorMapper;
import com.agustin.backend_dialysis_record.mapper.PatientMapper;
import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import com.agustin.backend_dialysis_record.repository.DoctorRepository;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.service.DoctorService;
import org.springframework.lang.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserAccountRepository userAccountRepository;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final PatientServiceImpl patientService;
    private final com.agustin.backend_dialysis_record.repository.DoctorPatientAccessRepository accessRepository;

    @Autowired
    public DoctorServiceImpl(DoctorRepository doctorRepository, PatientRepository patientRepository, UserAccountRepository userAccountRepository, DoctorMapper doctorMapper,
                             PatientMapper patientMapper, PatientServiceImpl patientService, com.agustin.backend_dialysis_record.repository.DoctorPatientAccessRepository accessRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userAccountRepository = userAccountRepository;
        this.doctorMapper = doctorMapper;
        this.patientService = patientService;
        this.patientMapper = patientMapper;
        this.accessRepository = accessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorDto> findAll() {
        return doctorRepository.findAll()
                .stream().map(doctorMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorDto findById(UUID id) {
        return doctorRepository.findById(id)
                .map(doctorMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));
    }

    @Override
        public DoctorDto create(DoctorDto doctorDto) {
            Doctor doctor = doctorMapper.toEntity(doctorDto);
            doctor = doctorRepository.save(doctor);
            return doctorMapper.toDto(doctor);
        }

    @Override
    public DoctorDto update(UUID id, DoctorDto doctorDto) {
        if (doctorDto.getId() != null && !doctorDto.getId().equals(id))
            throw new IllegalArgumentException("Path id and DTO id must match");

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));
        doctorMapper.updateEntityFromDTO(doctor, doctorDto);
        Doctor savedDoctor = doctorRepository.save(doctor);
        return doctorMapper.toDto(savedDoctor);
    }

    @Override
    public void delete(UUID id) {
        if (!doctorRepository.existsById(id))
            throw new RuntimeException("Doctor not found with id: " + id);
        doctorRepository.deleteById(id);
    }

    @Override
    public PatientDto addPatientToDoctor(UUID doctorId, UUID patientId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + doctorId));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));

        if (!accessRepository.existsByDoctorIdAndPatientId(doctorId, patientId)) {
            com.agustin.backend_dialysis_record.model.DoctorPatientAccess access = new com.agustin.backend_dialysis_record.model.DoctorPatientAccess();
            access.setDoctor(doctor);
            access.setPatient(patient);
            accessRepository.save(access);
        }

        return patientMapper.toDto(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> getPatientsByDoctor(UUID doctorId) {
        return accessRepository.findByDoctorId(doctorId).stream()
                .map(access -> patientMapper.toDto(access.getPatient()))
                .toList();
    }

    @Override
    public void removePatientFromDoctor(UUID doctorId, UUID patientId) {
        accessRepository.findByDoctorIdAndPatientId(doctorId, patientId)
                .ifPresent(accessRepository::delete);
    }

    @Override
    public DoctorDto activate(UUID doctorId) {
        Doctor doctor = doctorRepository.findByIdIncludingInactive(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + doctorId));
        doctor.setActive(true);
        return doctorMapper.toDto(doctorRepository.save(doctor));
    }

    @Override
    public DoctorMeDto getMyDoctor(UUID authId) {
        UserAccount ua = userAccountRepository.findByAuthId(authId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));

        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(ua.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Doctor not linked to this account"));

        DoctorDto base = findById(doctorId);
        DoctorMeDto dto = new DoctorMeDto();
        dto.setId(base.getId());
        dto.setName(base.getName());
        dto.setSurname(base.getSurname());
        dto.setEmail(ua.getEmail());
        dto.setRole(ua.getRole());
        
        List<PatientDto> patients = getPatientsByDoctor(doctorId);
        dto.setPatientCount(patients.size());
        return dto;
    }

    @Override
    public List<PatientDto> getMyPatients(UUID authId) {
        UserAccount ua = userAccountRepository.findByAuthId(authId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));

        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(ua.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Doctor not linked to this account"));

        return getPatientsByDoctor(doctorId);
    }

    @Override
    public PatientDto addPatientToMyDoctor(UUID authId, UUID patientId) {
        UserAccount ua = userAccountRepository.findByAuthId(authId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));

        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(ua.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Doctor not linked to this account"));

        return addPatientToDoctor(doctorId, patientId);
    }

    @Override
    public void removePatientFromMyDoctor(UUID authId, UUID patientId) {
        UserAccount ua = userAccountRepository.findByAuthId(authId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));

        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(ua.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Doctor not linked to this account"));

        removePatientFromDoctor(doctorId, patientId);
    }

}
