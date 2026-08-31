package com.agustin.backend_dialysis_record.repository;

import com.agustin.backend_dialysis_record.model.PatientInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientInvitationRepository extends JpaRepository<PatientInvitation, UUID> {
    List<PatientInvitation> findByDoctorId(UUID doctorId);
    List<PatientInvitation> findByPatientEmail(String email);
    List<PatientInvitation> findByPatientDni(Integer dni);
    List<PatientInvitation> findByPatientId(UUID patientId);
}
