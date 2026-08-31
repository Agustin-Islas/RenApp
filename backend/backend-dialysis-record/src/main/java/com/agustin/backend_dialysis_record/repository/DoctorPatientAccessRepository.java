package com.agustin.backend_dialysis_record.repository;

import com.agustin.backend_dialysis_record.model.DoctorPatientAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorPatientAccessRepository extends JpaRepository<DoctorPatientAccess, UUID> {
    List<DoctorPatientAccess> findByDoctorId(UUID doctorId);
    List<DoctorPatientAccess> findByPatientId(UUID patientId);
    Optional<DoctorPatientAccess> findByDoctorIdAndPatientId(UUID doctorId, UUID patientId);
    boolean existsByDoctorIdAndPatientId(UUID doctorId, UUID patientId);
}
