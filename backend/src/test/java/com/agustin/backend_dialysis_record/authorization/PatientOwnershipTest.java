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
 * Tests unitarios para AuthzService — Ownership de paciente.
 *
 * <p>Verifica que un paciente solo puede acceder a sus propios
 * datos y sesiones, no a los de otro paciente.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>S-01 — Paciente A intenta ver sesiones de Paciente B → rechazado</li>
 *   <li>S-02 — Paciente A intenta crear sesión para Paciente B → rechazado</li>
 *   <li>S-03 — Paciente A intenta editar sesión de Paciente B → rechazado</li>
 *   <li>S-04 — Paciente A intenta eliminar sesión de Paciente B → rechazado</li>
 *   <li>S-05 — Paciente A accede a su propio perfil → permitido</li>
 *   <li>S-06 — Paciente A intenta acceder a perfil de Paciente B → rechazado</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthzService — Ownership de paciente")
class PatientOwnershipTest {

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

    // Datos de prueba
    private UUID patientAAuthId;
    private UUID patientAId;
    private UUID patientBId;
    private UUID sessionOfPatientAId;
    private UUID sessionOfPatientBId;

    @BeforeEach
    void setUp() {
        patientAAuthId = UUID.randomUUID();
        patientAId = UUID.randomUUID();
        patientBId = UUID.randomUUID();
        sessionOfPatientAId = UUID.randomUUID();
        sessionOfPatientBId = UUID.randomUUID();
    }

    /**
     * Configura el SecurityContext para simular que el Paciente A
     * está autenticado y retorna su UserAccount con el patientId correcto.
     */
    private void authenticateAsPatientA() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(
                new TestingAuthenticationToken(patientAAuthId.toString(), null, "ROLE_PATIENT")
        );
        SecurityContextHolder.setContext(ctx);

        Patient patientA = new Patient();
        patientA.setId(patientAId);

        UserAccount ua = new UserAccount();
        ua.setId(UUID.randomUUID());
        ua.setAuthId(patientAAuthId);
        ua.setRole(UserRole.PATIENT);
        ua.setPatient(patientA);

        // Necesitamos un doctor nulo explícitamente para que el @PrePersist no falle
        // (solo en contexto de mock, no se ejecuta @PrePersist)

        when(userAccountRepository.findByAuthId(patientAAuthId)).thenReturn(Optional.of(ua));
    }

    // ═══════════════════════════════════════════════════════
    // Acceso a perfil de paciente
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("canAccessPatient — Acceso a perfil")
    class AccesoPerfilPaciente {

        /**
         * S-05 — Paciente A accede a su propio perfil → permitido
         */
        @Test
        @DisplayName("S-05 — Paciente accede a su propio perfil (/api/patients/me) → permitido")
        void pacienteAccedeSuPerfil_permitido() {
            authenticateAsPatientA();

            assertTrue(authzService.canAccessPatient(patientAId));
        }

        /**
         * S-06 — Paciente A intenta acceder a perfil de Paciente B → rechazado
         */
        @Test
        @DisplayName("S-06 — Paciente A intenta acceder a perfil de Paciente B → rechazado")
        void pacienteAccedePerfilDeOtro_rechazado() {
            authenticateAsPatientA();

            assertFalse(authzService.canAccessPatient(patientBId));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Acceso a sesiones
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("canAccessSession — Acceso a sesiones")
    class AccesoSesiones {

        /**
         * S-01 — Paciente A intenta ver sesiones de Paciente B → rechazado
         * (implícitamente cubre S-02, S-03, S-04 porque canAccessSession
         * delega a canAccessPatient via el patientId de la sesión)
         */
        @Test
        @DisplayName("S-01 — Paciente A intenta acceder a sesión de Paciente B → rechazado")
        void pacienteAccedeSesionDeOtro_rechazado() {
            authenticateAsPatientA();

            // La sesión pertenece al Paciente B
            when(sessionRepository.findPatientIdBySessionId(sessionOfPatientBId))
                    .thenReturn(Optional.of(patientBId));

            assertFalse(authzService.canAccessSession(sessionOfPatientBId));
        }

        @Test
        @DisplayName("Paciente A accede a su propia sesión → permitido")
        void pacienteAccedeSuSesion_permitido() {
            authenticateAsPatientA();

            when(sessionRepository.findPatientIdBySessionId(sessionOfPatientAId))
                    .thenReturn(Optional.of(patientAId));

            assertTrue(authzService.canAccessSession(sessionOfPatientAId));
        }

        @Test
        @DisplayName("Sesión inexistente → rechazado (no crash)")
        void sesionInexistente_rechazado() {
            UUID fakeSessionId = UUID.randomUUID();
            when(sessionRepository.findPatientIdBySessionId(fakeSessionId))
                    .thenReturn(Optional.empty());

            assertFalse(authzService.canAccessSession(fakeSessionId));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Helpers de rol
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Helpers de rol")
    class HelpersDeRol {

        @Test
        @DisplayName("isPatient() retorna true para paciente autenticado")
        void isPatient_retornaTrue() {
            authenticateAsPatientA();
            assertTrue(authzService.isPatient());
        }

        @Test
        @DisplayName("isDoctorOrAdmin() retorna false para paciente")
        void isDoctorOrAdmin_retornaFalse() {
            authenticateAsPatientA();
            assertFalse(authzService.isDoctorOrAdmin());
        }

        @Test
        @DisplayName("isAdmin() retorna false para paciente")
        void isAdmin_retornaFalse() {
            authenticateAsPatientA();
            assertFalse(authzService.isAdmin());
        }

        @Test
        @DisplayName("Sin autenticación todos los checks retornan false")
        void sinAuth_retornaFalse() {
            SecurityContextHolder.clearContext();

            // getCurrentUserAccount() retornará null porque no hay auth
            // Pero necesitamos manejar el caso donde auth.getName() es null
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(null);
            SecurityContextHolder.setContext(ctx);

            assertFalse(authzService.isPatient());
            assertFalse(authzService.isDoctorOrAdmin());
            assertFalse(authzService.isAdmin());
        }
    }
}
