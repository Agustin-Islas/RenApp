package com.agustin.backend_dialysis_record.repository;

import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByAuthId(UUID authId);

    boolean existsByAuthId(UUID authId);

    @Query("select ua from UserAccount ua where lower(trim(ua.email)) = lower(trim(:email))")
    Optional<UserAccount> findByNormalizedEmail(@Param("email") String email);

    @Query("select count(ua) > 0 from UserAccount ua where lower(trim(ua.email)) = lower(trim(:email))")
    boolean existsByNormalizedEmail(@Param("email") String email);

    boolean existsByIdAndPatient_Id(UUID userAccountId, UUID patientId);

    boolean existsByIdAndDoctor_Id(UUID userAccountId, UUID doctorId);

    @Query("select ua.doctor.id from UserAccount ua where ua.id = :userAccountId")
    Optional<UUID> findDoctorIdByUserAccountId(UUID userAccountId);

    @Query("select ua.patient.id from UserAccount ua where ua.id = :userAccountId")
    Optional<UUID> findPatientIdByUserAccountId(UUID userAccountId);

}
