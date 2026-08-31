package com.agustin.backend_dialysis_record.repository;

import com.agustin.backend_dialysis_record.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    @Query(value = "SELECT * FROM doctor WHERE id = :id", nativeQuery = true)
    Optional<Doctor> findByIdIncludingInactive(@Param("id") UUID id);
}

