package com.agustin.backend_dialysis_record.auth;

import com.agustin.backend_dialysis_record.dto.auth.RegisterDoctorRequest;
import com.agustin.backend_dialysis_record.dto.auth.RegisterPatientRequest;
import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import com.agustin.backend_dialysis_record.model.auth.UserRole;
import com.agustin.backend_dialysis_record.repository.DoctorRepository;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthService.
 *
 * <p>Verifica la lógica de registro de usuarios, normalización
 * de email y manejo de cuentas duplicadas.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>F-01 — Registro de paciente exitoso</li>
 *   <li>F-02 — Registro de doctor exitoso</li>
 *   <li>Plan de pruebas — Email duplicado normalizado</li>
 *   <li>Plan de pruebas — Cuenta duplicada (authId existente)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Lógica de registro")
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private PatientRepository patientRepo;
    @Mock
    private DoctorRepository doctorRepo;

    @InjectMocks
    private AuthService authService;

    private UUID authId;

    @BeforeEach
    void setUp() {
        authId = UUID.randomUUID();
    }

    // ═══════════════════════════════════════════════════════
    // Registro de paciente
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerPatient")
    class RegisterPatient {

        @Test
        @DisplayName("Registro exitoso crea Patient + UserAccount con email normalizado")
        void registroExitoso_creaEntidades() {
            // Arrange
            RegisterPatientRequest req = new RegisterPatientRequest(
                    "  Paciente@Email.COM  ",
                    "Juan", "Pérez",
                    12345678,
                    LocalDate.of(1985, 3, 15),
                    "Av. Siempre Viva 742",
                    1155554444L
            );

            when(userAccountRepository.existsByAuthId(authId)).thenReturn(false);
            when(userAccountRepository.findByNormalizedEmail(any())).thenReturn(Optional.empty());

            Patient savedPatient = new Patient();
            savedPatient.setId(UUID.randomUUID());
            when(patientRepo.save(any(Patient.class))).thenReturn(savedPatient);
            when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            authService.registerPatient(req, authId, "  Paciente@Email.COM  ");

            // Assert — se guardó el paciente
            ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
            verify(patientRepo).save(patientCaptor.capture());
            Patient captured = patientCaptor.getValue();
            assertEquals("Juan", captured.getName());
            assertEquals("Pérez", captured.getSurname());
            assertEquals(12345678, captured.getDni());

            // Assert — se guardó el UserAccount con email normalizado
            ArgumentCaptor<UserAccount> uaCaptor = ArgumentCaptor.forClass(UserAccount.class);
            verify(userAccountRepository).save(uaCaptor.capture());
            UserAccount ua = uaCaptor.getValue();
            assertEquals("paciente@email.com", ua.getEmail());
            assertEquals(UserRole.PATIENT, ua.getRole());
            assertEquals(authId, ua.getAuthId());
        }

        @Test
        @DisplayName("AuthId ya registrado lanza CONFLICT")
        void authIdDuplicado_lanzaConflict() {
            RegisterPatientRequest req = new RegisterPatientRequest(
                    "paciente@email.com", "Juan", "Pérez",
                    12345678, LocalDate.of(1985, 3, 15),
                    "Dir", 1155554444L
            );
            when(userAccountRepository.existsByAuthId(authId)).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> authService.registerPatient(req, authId, "paciente@email.com"));

            assertEquals(409, ex.getStatusCode().value());
        }

        @Test
        @DisplayName("Email existente vincula authId a cuenta existente (relink)")
        void emailExistente_revinculaAuthId() {
            RegisterPatientRequest req = new RegisterPatientRequest(
                    "existente@email.com", "Juan", "Pérez",
                    12345678, LocalDate.of(1985, 3, 15),
                    "Dir", 1155554444L
            );

            UserAccount existing = new UserAccount();
            existing.setId(UUID.randomUUID());
            existing.setEmail("existente@email.com");

            when(userAccountRepository.existsByAuthId(authId)).thenReturn(false);
            when(userAccountRepository.findByNormalizedEmail("existente@email.com"))
                    .thenReturn(Optional.of(existing));
            when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            authService.registerPatient(req, authId, "existente@email.com");

            // Assert — se actualizó el authId, NO se creó Patient nuevo
            verify(patientRepo, never()).save(any());
            verify(userAccountRepository).save(existing);
            assertEquals(authId, existing.getAuthId());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Registro de doctor
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerDoctor")
    class RegisterDoctor {

        @Test
        @DisplayName("Registro exitoso crea Doctor + UserAccount con email normalizado")
        void registroExitoso_creaEntidades() {
            RegisterDoctorRequest req = new RegisterDoctorRequest(
                    "  Doctor@Hospital.com  ",
                    "María",
                    "González"
            );

            when(userAccountRepository.existsByAuthId(authId)).thenReturn(false);
            when(userAccountRepository.findByNormalizedEmail(any())).thenReturn(Optional.empty());

            Doctor savedDoctor = new Doctor();
            savedDoctor.setId(UUID.randomUUID());
            when(doctorRepo.save(any(Doctor.class))).thenReturn(savedDoctor);
            when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            authService.registerDoctor(req, authId, "  Doctor@Hospital.com  ");

            // Assert
            ArgumentCaptor<Doctor> doctorCaptor = ArgumentCaptor.forClass(Doctor.class);
            verify(doctorRepo).save(doctorCaptor.capture());
            assertEquals("María", doctorCaptor.getValue().getName());

            ArgumentCaptor<UserAccount> uaCaptor = ArgumentCaptor.forClass(UserAccount.class);
            verify(userAccountRepository).save(uaCaptor.capture());
            UserAccount ua = uaCaptor.getValue();
            assertEquals("doctor@hospital.com", ua.getEmail());
            assertEquals(UserRole.DOCTOR, ua.getRole());
        }

        @Test
        @DisplayName("AuthId ya registrado lanza CONFLICT")
        void authIdDuplicado_lanzaConflict() {
            RegisterDoctorRequest req = new RegisterDoctorRequest(
                    "doc@email.com", "María", "González"
            );
            when(userAccountRepository.existsByAuthId(authId)).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> authService.registerDoctor(req, authId, "doc@email.com"));

            assertEquals(409, ex.getStatusCode().value());
        }
    }
}
