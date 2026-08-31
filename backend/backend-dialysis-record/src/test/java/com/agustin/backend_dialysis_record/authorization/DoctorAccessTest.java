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
 * Tests unitarios para AuthzService — Acceso del doctor.
 *
 * <p>Verifica que un doctor solo puede acceder a pacientes
 * asociados (via DoctorPatientAccess) y a sus propios recursos.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>S-07 — Doctor accede a sesiones de paciente asociado → permitido</li>
 *   <li>S-08 — Doctor accede a sesiones de paciente NO asociado → rechazado</li>
 *   <li>S-10 — Doctor accede a su lista de pacientes → solo asociados</li>
 *   <li>S-11 — Doctor intenta acceder a endpoints de otro doctor → rechazado</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthzService — Acceso del doctor")
class DoctorAccessTest {

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

    private UUID doctorAuthId;
    private UUID doctorId;
    private UUID associatedPatientId;
    private UUID unassociatedPatientId;
    private UUID otherDoctorId;

    @BeforeEach
    void setUp() {
        doctorAuthId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        associatedPatientId = UUID.randomUUID();
        unassociatedPatientId = UUID.randomUUID();
        otherDoctorId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsDoctor() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(
                new TestingAuthenticationToken(doctorAuthId.toString(), null, "ROLE_DOCTOR")
        );
        SecurityContextHolder.setContext(ctx);

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);

        UserAccount ua = new UserAccount();
        ua.setId(UUID.randomUUID());
        ua.setAuthId(doctorAuthId);
        ua.setRole(UserRole.DOCTOR);
        ua.setDoctor(doctor);

        when(userAccountRepository.findByAuthId(doctorAuthId)).thenReturn(Optional.of(ua));
    }

    // ═══════════════════════════════════════════════════════
    // Acceso a pacientes
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("canAccessPatient — Acceso a paciente")
    class AccesoPaciente {

        /**
         * S-07 — Doctor accede a paciente asociado → permitido
         */
        @Test
        @DisplayName("S-07 — Doctor accede a paciente asociado → permitido")
        void doctorAccedePacienteAsociado_permitido() {
            authenticateAsDoctor();
            when(doctorPatientAccessRepository.existsByDoctorIdAndPatientId(doctorId, associatedPatientId))
                    .thenReturn(true);

            assertTrue(authzService.canAccessPatient(associatedPatientId));
        }

        /**
         * S-08 — Doctor accede a paciente NO asociado → rechazado
         */
        @Test
        @DisplayName("S-08 — Doctor accede a paciente NO asociado → rechazado")
        void doctorAccedePacienteNoAsociado_rechazado() {
            authenticateAsDoctor();
            when(doctorPatientAccessRepository.existsByDoctorIdAndPatientId(doctorId, unassociatedPatientId))
                    .thenReturn(false);

            assertFalse(authzService.canAccessPatient(unassociatedPatientId));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Acceso a sesiones de paciente
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("canAccessSession — Doctor accede a sesiones vía paciente")
    class AccesoSesionesDoctor {

        @Test
        @DisplayName("S-07b — Doctor accede a sesión de paciente asociado → permitido")
        void doctorAccedeSesionDePacienteAsociado_permitido() {
            authenticateAsDoctor();
            UUID sessionId = UUID.randomUUID();

            when(sessionRepository.findPatientIdBySessionId(sessionId))
                    .thenReturn(Optional.of(associatedPatientId));
            when(doctorPatientAccessRepository.existsByDoctorIdAndPatientId(doctorId, associatedPatientId))
                    .thenReturn(true);

            assertTrue(authzService.canAccessSession(sessionId));
        }

        @Test
        @DisplayName("S-08b — Doctor accede a sesión de paciente NO asociado → rechazado")
        void doctorAccedeSesionDePacienteNoAsociado_rechazado() {
            authenticateAsDoctor();
            UUID sessionId = UUID.randomUUID();

            when(sessionRepository.findPatientIdBySessionId(sessionId))
                    .thenReturn(Optional.of(unassociatedPatientId));
            when(doctorPatientAccessRepository.existsByDoctorIdAndPatientId(doctorId, unassociatedPatientId))
                    .thenReturn(false);

            assertFalse(authzService.canAccessSession(sessionId));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Acceso a endpoints de otro doctor
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("canAccessDoctor — Acceso a recursos de doctor")
    class AccesoDoctor {

        /**
         * S-11 — Doctor intenta acceder a endpoints de otro doctor → rechazado
         */
        @Test
        @DisplayName("S-11 — Doctor accede a su propio recurso → permitido")
        void doctorAccedeSuRecurso_permitido() {
            authenticateAsDoctor();
            assertTrue(authzService.canAccessDoctor(doctorId));
        }

        @Test
        @DisplayName("S-11 — Doctor accede a recurso de otro doctor → rechazado")
        void doctorAccedeRecursoDeOtro_rechazado() {
            authenticateAsDoctor();
            assertFalse(authzService.canAccessDoctor(otherDoctorId));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Helpers de rol
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Helpers de rol — Doctor")
    class HelpersDeRolDoctor {

        @Test
        @DisplayName("isDoctorOrAdmin() retorna true para doctor")
        void isDoctorOrAdmin_retornaTrue() {
            authenticateAsDoctor();
            assertTrue(authzService.isDoctorOrAdmin());
        }

        @Test
        @DisplayName("isPatient() retorna false para doctor")
        void isPatient_retornaFalse() {
            authenticateAsDoctor();
            assertFalse(authzService.isPatient());
        }

        @Test
        @DisplayName("isAdmin() retorna false para doctor")
        void isAdmin_retornaFalse() {
            authenticateAsDoctor();
            assertFalse(authzService.isAdmin());
        }
    }
}
