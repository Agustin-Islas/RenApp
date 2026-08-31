package com.agustin.backend_dialysis_record.service.auth;

import com.agustin.backend_dialysis_record.dto.auth.RegisterDoctorRequest;
import com.agustin.backend_dialysis_record.dto.auth.RegisterPatientRequest;
import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import com.agustin.backend_dialysis_record.model.auth.UserRole;
import com.agustin.backend_dialysis_record.repository.DoctorRepository;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PatientRepository patientRepo;
    private final DoctorRepository doctorRepo;

    public AuthService(UserAccountRepository userAccountRepository, PatientRepository patientRepo, DoctorRepository doctorRepo) {
        this.userAccountRepository = userAccountRepository;
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
    }

    public void registerDoctor(RegisterDoctorRequest req, UUID authId, String email) {
        if (userAccountRepository.existsByAuthId(authId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor profile already linked to this auth account");
        }

        var existingByEmail = userAccountRepository.findByNormalizedEmail(email);
        if (existingByEmail.isPresent()) {
            UserAccount ua = existingByEmail.get();
            ua.setAuthId(authId);
            userAccountRepository.save(ua);
            return;
        }

        Doctor doctor = new Doctor();
        doctor.setName(req.name());
        doctor.setSurname(req.surname());
        doctor = doctorRepo.save(doctor);

        UserAccount ua = new UserAccount();
        ua.setEmail(email.trim().toLowerCase());
        ua.setAuthId(authId);
        ua.setRole(UserRole.DOCTOR);
        ua.setDoctor(doctor);
        userAccountRepository.save(ua);
    }

    public void registerPatient(RegisterPatientRequest req, UUID authId, String email) {
        if (userAccountRepository.existsByAuthId(authId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient profile already linked to this auth account");
        }

        var existingByEmail = userAccountRepository.findByNormalizedEmail(email);
        if (existingByEmail.isPresent()) {
            UserAccount ua = existingByEmail.get();
            ua.setAuthId(authId);
            userAccountRepository.save(ua);
            return;
        }

        Patient patient = new Patient();
        patient.setName(req.name());
        patient.setSurname(req.surname());
        patient.setDni(req.dni());
        patient.setDateOfBirth(req.dateOfBirth());
        patient.setAddress(req.address());
        patient.setNumber(req.number());
        patient = patientRepo.save(patient);

        UserAccount ua = new UserAccount();
        ua.setEmail(email.trim().toLowerCase());
        ua.setAuthId(authId);
        ua.setRole(UserRole.PATIENT);
        ua.setPatient(patient);
        userAccountRepository.save(ua);
    }

}
