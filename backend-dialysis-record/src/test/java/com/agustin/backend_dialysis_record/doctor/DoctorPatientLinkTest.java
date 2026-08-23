package com.agustin.backend_dialysis_record.doctor;

import com.agustin.backend_dialysis_record.controller.DoctorController;
import com.agustin.backend_dialysis_record.dto.DoctorMeDto;
import com.agustin.backend_dialysis_record.dto.PatientDto;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.security.authorization.AuthzService;
import com.agustin.backend_dialysis_record.service.DoctorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para DoctorController — Gestión de pacientes.
 *
 * <p>Verifica las operaciones de asociar, listar y desasociar
 * pacientes del doctor autenticado.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>F-28 — Ver lista de pacientes asociados</li>
 *   <li>F-29 — Agregar paciente desde selector</li>
 *   <li>F-31 — Desasociar paciente</li>
 * </ul>
 */
@WebMvcTest(DoctorController.class)
@AutoConfigureMockMvc
@DisplayName("DoctorController — Gestión de pacientes")
class DoctorPatientLinkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private AuthzService authzService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String DOCTOR_AUTH_ID = "123e4567-e89b-12d3-a456-426614174000";

    private PatientDto buildPatientDto(UUID id, String name, String surname) {
        PatientDto dto = new PatientDto();
        dto.setId(id);
        dto.setName(name);
        dto.setSurname(surname);
        dto.setDni(12345678);
        dto.setDateOfBirth(LocalDate.of(1985, 3, 15));
        dto.setAddress("Test Address");
        dto.setNumber(1L);
        return dto;
    }

    // ═══════════════════════════════════════════════════════
    // Listar pacientes asociados
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/doctors/me/patients")
    class ListarPacientes {

        /**
         * Caso de prueba: F-28
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Lista con pacientes del doctor
         */
        @Test
        @DisplayName("F-28 — Listar pacientes asociados retorna lista")
        void listarPacientes_retornaLista() throws Exception {
            UUID patientId1 = UUID.randomUUID();
            UUID patientId2 = UUID.randomUUID();

            when(doctorService.getMyPatients(UUID.fromString(DOCTOR_AUTH_ID)))
                    .thenReturn(List.of(
                            buildPatientDto(patientId1, "Juan", "Pérez"),
                            buildPatientDto(patientId2, "Ana", "López")
                    ));

            mockMvc.perform(get("/api/doctors/me/patients")
                            .with(jwt().jwt(j -> j.subject(DOCTOR_AUTH_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Juan"))
                    .andExpect(jsonPath("$[1].name").value("Ana"));
        }

        @Test
        @DisplayName("F-28 — Doctor sin pacientes retorna lista vacía")
        void sinPacientes_retornaListaVacia() throws Exception {
            when(doctorService.getMyPatients(UUID.fromString(DOCTOR_AUTH_ID)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/doctors/me/patients")
                            .with(jwt().jwt(j -> j.subject(DOCTOR_AUTH_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Agregar paciente
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/doctors/me/patients/{patientId}")
    class AgregarPaciente {

        /**
         * Caso de prueba: F-29
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Paciente aparece en la lista del doctor
         */
        @Test
        @DisplayName("F-29 — Agregar paciente retorna datos del paciente")
        void agregarPaciente_retornaPaciente() throws Exception {
            UUID patientId = UUID.randomUUID();
            PatientDto expected = buildPatientDto(patientId, "Juan", "Pérez");

            when(doctorService.addPatientToMyDoctor(
                    eq(UUID.fromString(DOCTOR_AUTH_ID)),
                    eq(patientId)))
                    .thenReturn(expected);

            mockMvc.perform(post("/api/doctors/me/patients/" + patientId)
                            .with(jwt().jwt(j -> j.subject(DOCTOR_AUTH_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Juan"));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Desasociar paciente
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/doctors/me/patients/{patientId}")
    class DesasociarPaciente {

        /**
         * Caso de prueba: F-31
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Paciente desaparece de la lista
         */
        @Test
        @DisplayName("F-31 — Desasociar paciente retorna 204 No Content")
        void desasociarPaciente_retorna204() throws Exception {
            UUID patientId = UUID.randomUUID();

            doNothing().when(doctorService).removePatientFromMyDoctor(
                    eq(UUID.fromString(DOCTOR_AUTH_ID)),
                    eq(patientId));

            mockMvc.perform(delete("/api/doctors/me/patients/" + patientId)
                            .with(jwt().jwt(j -> j.subject(DOCTOR_AUTH_ID))))
                    .andExpect(status().isNoContent());

            verify(doctorService).removePatientFromMyDoctor(
                    eq(UUID.fromString(DOCTOR_AUTH_ID)),
                    eq(patientId));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Doctor /me
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/doctors/me")
    class DoctorMe {

        @Test
        @DisplayName("GET /api/doctors/me retorna perfil del doctor")
        void getDoctorMe_retornaPerfil() throws Exception {
            DoctorMeDto meDto = new DoctorMeDto();
            meDto.setName("María");
            meDto.setSurname("González");

            when(doctorService.getMyDoctor(UUID.fromString(DOCTOR_AUTH_ID)))
                    .thenReturn(meDto);

            mockMvc.perform(get("/api/doctors/me")
                            .with(jwt().jwt(j -> j.subject(DOCTOR_AUTH_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("María"));
        }
    }
}
