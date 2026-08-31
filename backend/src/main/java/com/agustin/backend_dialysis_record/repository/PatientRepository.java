package com.agustin.backend_dialysis_record.repository;

import com.agustin.backend_dialysis_record.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    @Query(value = "SELECT * FROM patient WHERE id = :id", nativeQuery = true)
    Optional<Patient> findByIdIncludingInactive(@Param("id") UUID id);


}
