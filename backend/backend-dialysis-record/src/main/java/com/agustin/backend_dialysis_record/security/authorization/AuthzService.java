package com.agustin.backend_dialysis_record.security.authorization;

import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.SessionRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("authz")
public class AuthzService {

    private final UserAccountRepository userAccountRepository;
    private final PatientRepository patientRepository;
    private final SessionRepository sessionRepository;
    private final com.agustin.backend_dialysis_record.repository.DoctorPatientAccessRepository doctorPatientAccessRepository;

    public AuthzService(UserAccountRepository userAccountRepository,
                        PatientRepository patientRepository,
                        SessionRepository sessionRepository,
                        com.agustin.backend_dialysis_record.repository.DoctorPatientAccessRepository doctorPatientAccessRepository) {
        this.userAccountRepository = userAccountRepository;
        this.patientRepository = patientRepository;
        this.sessionRepository = sessionRepository;
        this.doctorPatientAccessRepository = doctorPatientAccessRepository;
    }

    // ── Helpers de rol (para @PreAuthorize en endpoints de listado/admin) ──

    /** Retorna true si el usuario autenticado tiene rol ADMIN. */
    public boolean isAdmin() {
        UserAccount ua = getCurrentUserAccount();
        return ua != null && "ADMIN".equals(ua.getRole().name());
    }

    /** Retorna true si el usuario autenticado tiene rol DOCTOR o ADMIN. */
    public boolean isDoctorOrAdmin() {
        UserAccount ua = getCurrentUserAccount();
        if (ua == null) return false;
        String role = ua.getRole().name();
        return "DOCTOR".equals(role) || "ADMIN".equals(role);
    }

    /** Retorna true si el usuario autenticado tiene rol PATIENT. */
    public boolean isPatient() {
        UserAccount ua = getCurrentUserAccount();
        return ua != null && "PATIENT".equals(ua.getRole().name());
    }

    // ── Acceso granular por recurso ──

    public boolean canAccessDoctor(UUID doctorId) {
        UserAccount userAccount = getCurrentUserAccount();
        if (userAccount == null) return false;

        if ("ADMIN".equals(userAccount.getRole().name())) return true;

        if ("DOCTOR".equals(userAccount.getRole().name())) {
            if (userAccount.getDoctor() == null) return false;
            return userAccount.getDoctor().getId().equals(doctorId);
        }

        return false;
    }

    public boolean canAccessPatient(UUID patientId) {
        UserAccount userAccount = getCurrentUserAccount();
        if (userAccount == null) return false;

        if ("ADMIN".equals(userAccount.getRole().name())) return true;

        if ("DOCTOR".equals(userAccount.getRole().name())) {
            if (userAccount.getDoctor() == null) return false;
            return doctorPatientAccessRepository.existsByDoctorIdAndPatientId(userAccount.getDoctor().getId(), patientId);
        }

        if ("PATIENT".equals(userAccount.getRole().name())) {
            if (userAccount.getPatient() == null) return false;
            return userAccount.getPatient().getId().equals(patientId);
        }

        return false;
    }

    public boolean canAccessSession(UUID sessionId) {
        UUID patientId = sessionRepository.findPatientIdBySessionId(sessionId).orElse(null);
        if (patientId == null) return false;
        return canAccessPatient(patientId);
    }

    // ── Método privado para obtener el UserAccount del usuario autenticado ──

    private UserAccount getCurrentUserAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        UUID authId = UUID.fromString(auth.getName());
        return userAccountRepository.findByAuthId(authId).orElse(null);
    }
}
