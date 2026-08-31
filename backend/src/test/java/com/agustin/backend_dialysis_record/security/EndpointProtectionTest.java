package com.agustin.backend_dialysis_record.security;

import com.agustin.backend_dialysis_record.controller.PatientController;
import com.agustin.backend_dialysis_record.controller.DoctorController;
import com.agustin.backend_dialysis_record.controller.SessionController;
import com.agustin.backend_dialysis_record.controller.PingController;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.security.authorization.AuthzService;
import com.agustin.backend_dialysis_record.service.DoctorService;
import com.agustin.backend_dialysis_record.service.PatientService;
import com.agustin.backend_dialysis_record.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.agustin.backend_dialysis_record.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para protección de endpoints.
 *
 * <p>Verifica que los endpoints protegidos rechazan requests
 * sin autenticación y que los endpoints públicos son accesibles.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>S-14 — Usuario sin autenticar accede a endpoint protegido → 401</li>
 *   <li>S-15 — Usuario sin autenticar accede a endpoints públicos (/ping) → 200</li>
 *   <li>S-21 — Request sin header Authorization a endpoint protegido → 401</li>
 * </ul>
 */
@WebMvcTest({PatientController.class, DoctorController.class, SessionController.class, PingController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("Protección de endpoints")
class EndpointProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;
    @MockitoBean
    private SessionService sessionService;
    @MockitoBean
    private DoctorService doctorService;
    @MockitoBean
    private UserAccountRepository userAccountRepository;
    @MockitoBean
    private AuthzService authzService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ═══════════════════════════════════════════════════════
    // Endpoints protegidos sin auth → 401
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("S-14/S-21 — Endpoints protegidos sin auth")
    class EndpointsProtegidosSinAuth {

        @Test
        @DisplayName("S-21 — GET /api/patients/me sin auth → 401")
        void patientMeSinAuth_retorna401() throws Exception {
            mockMvc.perform(get("/api/patients/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("S-21 — GET /api/doctors/me sin auth → 401")
        void doctorMeSinAuth_retorna401() throws Exception {
            mockMvc.perform(get("/api/doctors/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("S-21 — GET /api/sessions sin auth → 401")
        void sessionsSinAuth_retorna401() throws Exception {
            mockMvc.perform(get("/api/sessions"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("S-14 — GET /api/patients/{id} sin auth → 401")
        void patientByIdSinAuth_retorna401() throws Exception {
            mockMvc.perform(get("/api/patients/123e4567-e89b-12d3-a456-426614174000"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("S-14 — GET /api/doctors/{id}/patients sin auth → 401")
        void doctorPatientsSinAuth_retorna401() throws Exception {
            mockMvc.perform(get("/api/doctors/123e4567-e89b-12d3-a456-426614174000/patients"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Endpoints públicos accesibles sin auth
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("S-15 — Endpoints públicos")
    class EndpointsPublicos {

        /**
         * S-15 — /ping es accesible sin autenticación.
         *
         * <p>Nota: En @WebMvcTest la SecurityConfig real carga
         * y /ping está configurado como permitAll en la cadena.</p>
         */
        @Test
        @DisplayName("S-15 — GET /ping sin auth → 200")
        void pingSinAuth_retorna200() throws Exception {
            mockMvc.perform(get("/ping"))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Autorización por rol — @PreAuthorize
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("S-12 — Autorización por rol en endpoints")
    class AutorizacionPorRol {

        /**
         * S-12 — Paciente intenta acceder a GET /api/sessions (requiere ADMIN).
         * Con JWT válido pero authzService retorna false → 403
         */
        @Test
        @DisplayName("S-12 — Usuario autenticado sin rol ADMIN en GET /api/sessions → 403")
        void usuarioSinRolAdmin_retorna403() throws Exception {
            when(authzService.isAdmin()).thenReturn(false);

            mockMvc.perform(get("/api/sessions")
                            .with(jwt().jwt(j -> j.subject("123e4567-e89b-12d3-a456-426614174000"))))
                    .andExpect(status().isForbidden());
        }
    }
}
