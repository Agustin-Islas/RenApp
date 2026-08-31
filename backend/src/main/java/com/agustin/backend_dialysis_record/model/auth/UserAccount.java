package com.agustin.backend_dialysis_record.model.auth;

import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.Patient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "user_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_account_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
public class UserAccount {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private UUID authId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    // LEGACY: Columna conservada temporalmente. Los hashes bcrypt de usuarios
    // migrados permanecen como evidencia de continuidad. Nuevos registros no la usan.
    // Fase futura: ALTER TABLE user_account DROP COLUMN password_hash;
    @Column(name = "password_hash")
    private String passwordHash;

    @OneToOne(fetch = FetchType.LAZY)
    private Doctor doctor;

    @OneToOne(fetch = FetchType.LAZY)
    private Patient patient;

    @PrePersist
    @PreUpdate
    void validate() {
        if (id == null) id = UUID.randomUUID();

        boolean hasDoctor = doctor != null;
        boolean hasPatient = patient != null;

        if (hasDoctor == hasPatient) { // inválido
            throw new IllegalStateException("UserAccount must be linked to exactly one: Doctor OR Patient");
        }
    }
}
