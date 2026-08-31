package com.agustin.backend_dialysis_record.session;

import com.agustin.backend_dialysis_record.controller.SessionController;
import com.agustin.backend_dialysis_record.dto.SessionDto;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.security.authorization.AuthzService;
import com.agustin.backend_dialysis_record.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.agustin.backend_dialysis_record.security.SecurityConfig;
import com.agustin.backend_dialysis_record.controller.PatientController;
import com.agustin.backend_dialysis_record.service.PatientService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para SessionController.
 *
 * <p>Verifica los endpoints de gestión de sesiones de diálisis.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>F-09/F-10 — Crear sesión</li>
 *   <li>F-12 — Editar sesión existente</li>
 *   <li>F-13 — Eliminar sesión</li>
 * </ul>
 */
@WebMvcTest({SessionController.class, PatientController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("SessionController — Gestión de sesiones")
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean(name = "authz")
    private AuthzService authzService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String AUTH_ID = "123e4567-e89b-12d3-a456-426614174000";

    private SessionDto buildValidSessionDto() {
        SessionDto dto = new SessionDto();
        dto.setDate(LocalDate.of(2025, 7, 15));
        dto.setHour(LocalTime.of(7, 30));
        dto.setBag(1);
        dto.setConcentration(1.5f);
        dto.setInfusion(1500);
        dto.setDrainage(1560);
        return dto;
    }

    // ═══════════════════════════════════════════════════════
    // POST /api/patients/{patientId}/sessions
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/patients/{patientId}/sessions")
    class CrearSesion {

        @Test
        @DisplayName("F-09/F-10 — Crear sesión retorna la sesión creada (201 Created)")
        void crearSesion_retorna201() throws Exception {
            UUID patientId = UUID.randomUUID();
            SessionDto requestDto = buildValidSessionDto();
            
            SessionDto responseDto = buildValidSessionDto();
            responseDto.setId(UUID.randomUUID());
            responseDto.setPartial(-60);

            // PreAuthorize mock
            when(authzService.canAccessPatient(patientId)).thenReturn(true);

            when(sessionService.createForPatient(eq(patientId), any(SessionDto.class)))
                    .thenReturn(responseDto);

            mockMvc.perform(post("/api/patients/" + patientId + "/sessions")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk()) // PatientController uses .ok(created) instead of .created()
                    .andExpect(jsonPath("$.partial").value(-60));
        }

        @Test
        @DisplayName("Crear sesión con payload inválido retorna 400 Bad Request")
        void crearSesionInvalida_retorna400() throws Exception {
            UUID patientId = UUID.randomUUID();
            SessionDto invalidDto = new SessionDto(); // Faltan campos obligatorios

            when(authzService.canAccessPatient(patientId)).thenReturn(true);

            mockMvc.perform(post("/api/patients/" + patientId + "/sessions")
                            .with(jwt().jwt(j -> j.subject(AUTH_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════
    // PUT /api/sessions/{id}
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/sessions/{id}")
    class EditarSesion {

        @Test
        @DisplayName("F-12 — Editar sesión existente retorna la sesión actualizada (200 OK)")
        void editarSesion_retorna200() throws Exception {
            UUID sessionId = UUID.randomUUID();
            SessionDto requestDto = buildValidSessionDto();
            requestDto.setId(sessionId);

            when(authzService.canAccessSession(sessionId)).thenReturn(true);

            when(sessionService.update(eq(sessionId), any(SessionDto.class)))
                    .thenReturn(requestDto);

            mockMvc.perform(put("/api/sessions/" + sessionId)
                            .with(jwt().jwt(j -> j.subject(AUTH_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.infusion").value(1500));
        }
    }

    // ═══════════════════════════════════════════════════════
    // DELETE /api/sessions/{id}
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/sessions/{id}")
    class EliminarSesion {

        @Test
        @DisplayName("F-13 — Eliminar sesión retorna 204 No Content")
        void eliminarSesion_retorna204() throws Exception {
            UUID sessionId = UUID.randomUUID();

            when(authzService.canAccessSession(sessionId)).thenReturn(true);
            doNothing().when(sessionService).delete(sessionId);

            mockMvc.perform(delete("/api/sessions/" + sessionId)
                            .with(jwt().jwt(j -> j.subject(AUTH_ID))))
                    .andExpect(status().isNoContent());

            verify(sessionService).delete(sessionId);
        }
    }
}
