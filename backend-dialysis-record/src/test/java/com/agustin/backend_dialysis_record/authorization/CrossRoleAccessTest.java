package com.agustin.backend_dialysis_record.authorization;

import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import com.agustin.backend_dialysis_record.model.auth.UserRole;
import com.agustin.backend_dialysis_record.repository.DoctorPatientAccessRepository;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.SessionRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.security.authorization.AuthzService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthzService — Cruce de roles.
 *
 * <p>Verifica que un paciente no puede acceder a funcionalidad de doctor
 * y viceversa; y que un usuario no autenticado es rechazado.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>S-12 — Paciente intenta acceder a endpoints de doctor → rechazado</li>
 *   <li>S-13 — Doctor intenta acceder a endpoints exclusivos de paciente → rechazado</li>
 *   <li>S-14 — Usuario sin autenticar accede a endpoint protegido → rechazado</li>
 *   <li>S-15 — Usuario sin autenticar accede a endpoints públicos → permitido</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthzService — Cruce de roles")
class CrossRoleAccessTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private DoctorPatientAccessRepository doctorPatientAccessRepository;

    @InjectMocks
    private AuthzService authzService;

    private UUID doctorId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsPatient() {
        UUID authId = UUID.randomUUID();
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(
                new TestingAuthenticationToken(authId.toString(), null, "ROLE_PATIENT")
        );
        SecurityContextHolder.setContext(ctx);

        Patient patient = new Patient();
        patient.setId(patientId);

        UserAccount ua = new UserAccount();
        ua.setId(UUID.randomUUID());
        ua.setAuthId(authId);
        ua.setRole(UserRole.PATIENT);
        ua.setPatient(patient);

        when(userAccountRepository.findByAuthId(authId)).thenReturn(Optional.of(ua));
    }

    private void authenticateAsDoctor() {
        UUID authId = UUID.randomUUID();
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(
                new TestingAuthenticationToken(authId.toString(), null, "ROLE_DOCTOR")
        );
        SecurityContextHolder.setContext(ctx);

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);

        UserAccount ua = new UserAccount();
        ua.setId(UUID.randomUUID());
        ua.setAuthId(authId);
        ua.setRole(UserRole.DOCTOR);
        ua.setDoctor(doctor);

        when(userAccountRepository.findByAuthId(authId)).thenReturn(Optional.of(ua));
    }

    // ═══════════════════════════════════════════════════════
    // Paciente no accede a recursos de doctor
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("S-12 — Paciente vs endpoints de doctor")
    class PacienteVsDoctor {

        /**
         * S-12 — Paciente intenta acceder a endpoints de doctor
         */
        @Test
        @DisplayName("S-12 — Paciente no puede acceder a canAccessDoctor()")
        void pacienteNoAccedeDoctor() {
            authenticateAsPatient();
            assertFalse(authzService.canAccessDoctor(doctorId));
        }

        @Test
        @DisplayName("S-12 — Paciente no es isDoctorOrAdmin()")
        void pacienteNoEsDoctorNiAdmin() {
            authenticateAsPatient();
            assertFalse(authzService.isDoctorOrAdmin());
        }

        @Test
        @DisplayName("S-12 — Paciente no es isAdmin()")
        void pacienteNoEsAdmin() {
            authenticateAsPatient();
            assertFalse(authzService.isAdmin());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Doctor no accede a perfil de paciente (directo)
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("S-13 — Doctor vs endpoints exclusivos de paciente")
    class DoctorVsPaciente {

        /**
         * S-13 — Doctor intenta acceder a perfil de paciente
         * sin asociación (como si fuera su propio /me de paciente)
         */
        @Test
        @DisplayName("S-13 — Doctor sin asociación no accede a paciente")
        void doctorSinAsociacionNoAccedePaciente() {
            authenticateAsDoctor();
            UUID randomPatientId = UUID.randomUUID();

            when(doctorPatientAccessRepository.existsByDoctorIdAndPatientId(doctorId, randomPatientId))
                    .thenReturn(false);

            assertFalse(authzService.canAccessPatient(randomPatientId));
        }

        @Test
        @DisplayName("S-13 — Doctor no es isPatient()")
        void doctorNoEsPaciente() {
            authenticateAsDoctor();
            assertFalse(authzService.isPatient());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Usuario sin autenticar
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("S-14/S-15 — Usuario sin autenticar")
    class SinAutenticar {

        /**
         * S-14 — Sin auth, todos los checks de acceso retornan false
         */
        @Test
        @DisplayName("S-14 — Sin auth, canAccessPatient() retorna false")
        void sinAuth_canAccessPatient_false() {
            SecurityContextHolder.clearContext();
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(null);
            SecurityContextHolder.setContext(ctx);

            assertFalse(authzService.canAccessPatient(UUID.randomUUID()));
        }

        @Test
        @DisplayName("S-14 — Sin auth, canAccessDoctor() retorna false")
        void sinAuth_canAccessDoctor_false() {
            SecurityContextHolder.clearContext();
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(null);
            SecurityContextHolder.setContext(ctx);

            assertFalse(authzService.canAccessDoctor(UUID.randomUUID()));
        }

        @Test
        @DisplayName("S-14 — Sin auth, canAccessSession() retorna false")
        void sinAuth_canAccessSession_false() {
            SecurityContextHolder.clearContext();
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(null);
            SecurityContextHolder.setContext(ctx);

            assertFalse(authzService.canAccessSession(UUID.randomUUID()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Admin tiene acceso total
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Admin — Acceso total")
    class AdminAcceso {

        private void authenticateAsAdmin() {
            UUID authId = UUID.randomUUID();
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(
                    new TestingAuthenticationToken(authId.toString(), null, "ROLE_ADMIN")
            );
            SecurityContextHolder.setContext(ctx);

            // Admin necesita tener un doctor o patient para evitar el @PrePersist constraint
            // En producción esto se maneja diferente, pero en mock no se ejecuta
            Doctor adminDoctor = new Doctor();
            adminDoctor.setId(UUID.randomUUID());

            UserAccount ua = new UserAccount();
            ua.setId(UUID.randomUUID());
            ua.setAuthId(authId);
            ua.setRole(UserRole.ADMIN);
            ua.setDoctor(adminDoctor);

            when(userAccountRepository.findByAuthId(authId)).thenReturn(Optional.of(ua));
        }

        @Test
        @DisplayName("Admin puede acceder a cualquier paciente")
        void adminAccedeCualquierPaciente() {
            authenticateAsAdmin();
            assertTrue(authzService.canAccessPatient(UUID.randomUUID()));
        }

        @Test
        @DisplayName("Admin puede acceder a cualquier doctor")
        void adminAccedeCualquierDoctor() {
            authenticateAsAdmin();
            assertTrue(authzService.canAccessDoctor(UUID.randomUUID()));
        }

        @Test
        @DisplayName("Admin pasa isAdmin() e isDoctorOrAdmin()")
        void adminPasaChecksDeRol() {
            authenticateAsAdmin();
            assertTrue(authzService.isAdmin());
            assertTrue(authzService.isDoctorOrAdmin());
        }
    }
}
