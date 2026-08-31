package com.agustin.backend_dialysis_record.auth;

import com.agustin.backend_dialysis_record.controller.AuthController;
import com.agustin.backend_dialysis_record.dto.auth.RegisterDoctorRequest;
import com.agustin.backend_dialysis_record.dto.auth.RegisterPatientRequest;
import com.agustin.backend_dialysis_record.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.agustin.backend_dialysis_record.security.SecurityConfig;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para AuthController.
 *
 * <p>Valida los endpoints de registro de pacientes y doctores
 * a nivel HTTP, incluyendo validaciones de request body y
 * manejo de errores.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>F-01 — Registro de paciente con datos válidos</li>
 *   <li>F-02 — Registro de doctor con datos válidos</li>
 * </ul>
 *
 * @see com.agustin.backend_dialysis_record.controller.AuthController
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("AuthController — Registro de usuarios")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ── Constantes reutilizables ──

    private static final String AUTH_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String EMAIL = "testuser@email.com";

    // ── Helpers para construir requests válidos ──

    private String validPatientRequestJson() throws Exception {
        return objectMapper.writeValueAsString(
                new RegisterPatientRequest(
                        "paciente@email.com",
                        "Juan",
                        "Pérez",
                        12345678,
                        LocalDate.of(1985, 3, 15),
                        "Av. Siempre Viva 742",
                        1155554444L
                )
        );
    }

    private String validDoctorRequestJson() throws Exception {
        return objectMapper.writeValueAsString(
                new RegisterDoctorRequest(
                        "doctor@email.com",
                        "María",
                        "González"
                )
        );
    }

    // ═══════════════════════════════════════════════════════
    // Registro de paciente
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /auth/register/patient")
    class RegistroPatient {

        /**
         * Caso de prueba: F-01
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Cuenta creada, retorna 200 OK
         */
        @Test
        @DisplayName("F-01 — Registro de paciente con datos válidos retorna 200")
        void registroPacienteExitoso_retorna200() throws Exception {
            doNothing().when(authService).registerPatient(
                    any(RegisterPatientRequest.class),
                    eq(UUID.fromString(AUTH_ID)),
                    any(String.class)
            );

            mockMvc.perform(post("/auth/register/patient")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID).claim("email", EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPatientRequestJson()))
                    .andExpect(status().isOk());

            verify(authService, times(1)).registerPatient(
                    any(RegisterPatientRequest.class),
                    eq(UUID.fromString(AUTH_ID)),
                    any(String.class)
            );
        }

        @Test
        @DisplayName("F-01b — Registro de paciente duplicado retorna 409 Conflict")
        void registroPacienteDuplicado_retorna409() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                    "Patient profile already linked to this auth account"))
                    .when(authService).registerPatient(
                            any(RegisterPatientRequest.class),
                            eq(UUID.fromString(AUTH_ID)),
                            any(String.class)
                    );

            mockMvc.perform(post("/auth/register/patient")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID).claim("email", EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPatientRequestJson()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Registro de paciente sin nombre retorna 400")
        void registroPacienteSinNombre_retorna400() throws Exception {
            String invalidJson = """
                    {
                      "email": "paciente@email.com",
                      "surname": "Pérez",
                      "dni": 12345678,
                      "dateOfBirth": "1985-03-15",
                      "address": "Av. Siempre Viva 742"
                    }
                    """;

            mockMvc.perform(post("/auth/register/patient")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID).claim("email", EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Registro de paciente con DNI fuera de rango retorna 400")
        void registroPacienteConDniInvalido_retorna400() throws Exception {
            String invalidJson = """
                    {
                      "email": "paciente@email.com",
                      "name": "Juan",
                      "surname": "Pérez",
                      "dni": 123,
                      "dateOfBirth": "1985-03-15",
                      "address": "Av. Siempre Viva 742",
                      "number": 1155554444
                    }
                    """;

            mockMvc.perform(post("/auth/register/patient")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID).claim("email", EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Registro de paciente sin autenticación retorna 401")
        void registroPacienteSinAuth_retorna401() throws Exception {
            mockMvc.perform(post("/auth/register/patient")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPatientRequestJson()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Registro de doctor
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /auth/register/doctor")
    class RegistroDoctor {

        /**
         * Caso de prueba: F-02
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Cuenta creada, retorna 200 OK
         */
        @Test
        @DisplayName("F-02 — Registro de doctor con datos válidos retorna 200")
        void registroDoctorExitoso_retorna200() throws Exception {
            doNothing().when(authService).registerDoctor(
                    any(RegisterDoctorRequest.class),
                    eq(UUID.fromString(AUTH_ID)),
                    any(String.class)
            );

            mockMvc.perform(post("/auth/register/doctor")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID).claim("email", EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validDoctorRequestJson()))
                    .andExpect(status().isOk());

            verify(authService, times(1)).registerDoctor(
                    any(RegisterDoctorRequest.class),
                    eq(UUID.fromString(AUTH_ID)),
                    any(String.class)
            );
        }

        @Test
        @DisplayName("F-02b — Registro de doctor duplicado retorna 409 Conflict")
        void registroDoctorDuplicado_retorna409() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                    "Doctor profile already linked to this auth account"))
                    .when(authService).registerDoctor(
                            any(RegisterDoctorRequest.class),
                            eq(UUID.fromString(AUTH_ID)),
                            any(String.class)
                    );

            mockMvc.perform(post("/auth/register/doctor")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID).claim("email", EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validDoctorRequestJson()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Registro de doctor sin email retorna 400")
        void registroDoctorSinEmail_retorna400() throws Exception {
            String invalidJson = """
                    {
                      "name": "María",
                      "surname": "González"
                    }
                    """;

            mockMvc.perform(post("/auth/register/doctor")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID).claim("email", EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Registro de doctor sin autenticación retorna 401")
        void registroDoctorSinAuth_retorna401() throws Exception {
            mockMvc.perform(post("/auth/register/doctor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validDoctorRequestJson()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
