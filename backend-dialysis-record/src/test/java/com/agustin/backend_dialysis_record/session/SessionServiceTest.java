package com.agustin.backend_dialysis_record.session;

import com.agustin.backend_dialysis_record.dto.SessionDto;
import com.agustin.backend_dialysis_record.dto.SessionSummaryDto;
import com.agustin.backend_dialysis_record.mapper.SessionMapper;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.Session;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.SessionRepository;
import com.agustin.backend_dialysis_record.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SessionServiceImpl.
 *
 * <p>Verifica la lógica de negocio de sesiones de diálisis:
 * cálculo de parcial, validación de concentraciones,
 * jornada clínica y consultas/resúmenes.</p>
 *
 * <h3>Casos de prueba de la tesis cubiertos:</h3>
 * <ul>
 *   <li>F-09 — Crear sesión con datos completos</li>
 *   <li>F-10 — Crear sesión con concentración personalizada</li>
 *   <li>F-11 — Rechazar concentración no permitida</li>
 *   <li>F-14 — Cálculo automático de parcial (infusión − drenaje)</li>
 *   <li>F-15 — Sesión post-medianoche → jornada clínica del día anterior</li>
 *   <li>F-16 — Indicador de cambio nocturno (clinicalDate ≠ date)</li>
 *   <li>F-17 a F-21 — Consultas por día, rango, resúmenes</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService — Lógica de negocio de sesiones")
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private SessionMapper sessionMapper;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private Patient testPatient;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        testPatient = new Patient();
        testPatient.setId(patientId);
        testPatient.setName("Juan");
        testPatient.setSurname("Pérez");
        testPatient.setCustomConcentrations(new ArrayList<>(List.of(4.25f)));
    }

    // ═══════════════════════════════════════════════════════
    // Cálculo de partial (infusión − drenaje)
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cálculo de partial")
    class CalculoPartial {

        /**
         * Caso de prueba: F-14
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: partial = infusion - drainage
         */
        @Test
        @DisplayName("F-14 — partial = infusión − drenaje (UF negativa = ganancia)")
        void partialSeCalculaCorrectamente() {
            Session session = new Session(
                    LocalDate.of(2025, 7, 15), LocalTime.of(7, 30),
                    1, 1.5f, 1500, 1560, "normal"
            );

            session.prePersist();

            // partial = 1500 - 1560 = -60 (paciente ganó 60ml)
            assertEquals(-60, session.getPartial());
        }

        @Test
        @DisplayName("F-14 — partial positivo cuando infusión > drenaje")
        void partialPositivoCuandoInfusionMayor() {
            Session session = new Session(
                    LocalDate.of(2025, 7, 15), LocalTime.of(12, 0),
                    2, 1.5f, 2000, 1800, null
            );

            session.prePersist();

            assertEquals(200, session.getPartial());
        }

        @Test
        @DisplayName("F-14 — partial cero cuando infusión = drenaje")
        void partialCeroCuandoIguales() {
            Session session = new Session(
                    LocalDate.of(2025, 7, 15), LocalTime.of(18, 0),
                    3, 2.3f, 1500, 1500, null
            );

            session.prePersist();

            assertEquals(0, session.getPartial());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Jornada clínica (clinicalDate)
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Jornada clínica — Corte a las 05:00")
    class JornadaClinica {

        /**
         * Caso de prueba: F-15
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Sesión de 00:30 → clinicalDate = día anterior
         */
        @Test
        @DisplayName("F-15 — Sesión a las 00:30 se asigna al día anterior")
        void sesionPostMedianoche_diaAnterior() {
            Session session = new Session(
                    LocalDate.of(2025, 7, 16), LocalTime.of(0, 30),
                    4, 1.5f, 1500, 1575, null
            );

            session.prePersist();

            assertEquals(LocalDate.of(2025, 7, 15), session.getClinicalDate());
        }

        @Test
        @DisplayName("F-15 — Sesión a las 04:59 se asigna al día anterior")
        void sesionAntesDelCorte_diaAnterior() {
            Session session = new Session(
                    LocalDate.of(2025, 7, 16), LocalTime.of(4, 59),
                    4, 1.5f, 1500, 1550, null
            );

            session.prePersist();

            assertEquals(LocalDate.of(2025, 7, 15), session.getClinicalDate());
        }

        @Test
        @DisplayName("Sesión a las 05:00 exactas se asigna al mismo día")
        void sesionEnElCorte_mismoDia() {
            Session session = new Session(
                    LocalDate.of(2025, 7, 16), LocalTime.of(5, 0),
                    1, 1.5f, 1500, 1560, null
            );

            session.prePersist();

            assertEquals(LocalDate.of(2025, 7, 16), session.getClinicalDate());
        }

        @Test
        @DisplayName("F-16 — Sesión diurna: clinicalDate = date (sin badge nocturno)")
        void sesionDiurna_mismoDia() {
            Session session = new Session(
                    LocalDate.of(2025, 7, 16), LocalTime.of(12, 0),
                    2, 1.5f, 1500, 1585, null
            );

            session.prePersist();

            assertEquals(LocalDate.of(2025, 7, 16), session.getClinicalDate());
            // clinicalDate == date → no hay badge de turno noche
            assertEquals(session.getDate(), session.getClinicalDate());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Validación de concentraciones
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validación de concentraciones")
    class ValidacionConcentraciones {

        /**
         * Caso de prueba: F-10
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Concentración personalizada aceptada
         */
        @Test
        @DisplayName("F-10 — Concentración personalizada (4.25%) es aceptada")
        void concentracionPersonalizada_aceptada() {
            SessionDto dto = buildSessionDto(4.25f);
            Session entity = buildSessionEntity(4.25f);

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(testPatient));
            when(sessionMapper.toEntity(dto)).thenReturn(entity);
            when(sessionRepository.save(any(Session.class))).thenReturn(entity);
            when(sessionMapper.toDto(entity)).thenReturn(dto);

            SessionDto result = sessionService.createForPatient(patientId, dto);

            assertNotNull(result);
            verify(sessionRepository).save(any(Session.class));
        }

        /**
         * Caso de prueba: F-09
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Concentraciones fijas (1.5%, 2.3%, 3.8%) aceptadas
         */
        @ParameterizedTest(name = "Concentración fija {0}% aceptada")
        @ValueSource(floats = {1.5f, 2.3f, 3.8f})
        @DisplayName("F-09 — Concentraciones fijas son aceptadas")
        void concentracionesFijas_aceptadas(float concentration) {
            SessionDto dto = buildSessionDto(concentration);
            Session entity = buildSessionEntity(concentration);

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(testPatient));
            when(sessionMapper.toEntity(dto)).thenReturn(entity);
            when(sessionRepository.save(any(Session.class))).thenReturn(entity);
            when(sessionMapper.toDto(entity)).thenReturn(dto);

            SessionDto result = sessionService.createForPatient(patientId, dto);

            assertNotNull(result);
        }

        /**
         * Caso de prueba: F-11
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         * Resultado esperado: Concentración 5.0% rechazada con error
         */
        @Test
        @DisplayName("F-11 — Concentración no permitida (5.0%) es rechazada")
        void concentracionNoPermitida_rechazada() {
            SessionDto dto = buildSessionDto(5.0f);
            Session entity = buildSessionEntity(5.0f);

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(testPatient));
            when(sessionMapper.toEntity(dto)).thenReturn(entity);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> sessionService.createForPatient(patientId, dto));

            assertTrue(ex.getMessage().contains("concentration is not allowed"));
            verify(sessionRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════
    // Consultas y resúmenes
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Consultas por día/rango y resúmenes")
    class ConsultasYResumenes {

        /**
         * Casos de prueba: F-17 a F-21
         * Evaluación: Funcional (Capítulo 6, Sección 6.2)
         */
        @Test
        @DisplayName("F-17/F-18 — Consulta sesiones por día retorna lista")
        void consultaPorDia_retornaLista() {
            LocalDate day = LocalDate.of(2025, 7, 15);
            Session s1 = buildSessionEntity(1.5f);
            Session s2 = buildSessionEntity(1.5f);
            SessionDto dto1 = buildSessionDto(1.5f);
            SessionDto dto2 = buildSessionDto(1.5f);

            when(sessionRepository.findByPatientIdAndClinicalDateOrderByDateDescHourDesc(patientId, day))
                    .thenReturn(List.of(s1, s2));
            when(sessionMapper.toDto(s1)).thenReturn(dto1);
            when(sessionMapper.toDto(s2)).thenReturn(dto2);

            List<SessionDto> result = sessionService.findSessionsByDay(patientId, day);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("F-20 — Consulta sesiones por rango de fechas")
        void consultaPorRango_retornaLista() {
            LocalDate start = LocalDate.of(2025, 7, 1);
            LocalDate end = LocalDate.of(2025, 7, 31);

            when(sessionRepository.findByPatientIdAndClinicalDateBetweenOrderByClinicalDateDescDateDescHourDesc(
                    patientId, start, end)).thenReturn(List.of());

            List<SessionDto> result = sessionService.findSessionsByPatientIdAndDateRange(
                    patientId, start, end);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Consulta por rango inválido (start > end) lanza error")
        void consultaRangoInvalido_lanzaError() {
            LocalDate start = LocalDate.of(2025, 8, 1);
            LocalDate end = LocalDate.of(2025, 7, 1);

            assertThrows(IllegalArgumentException.class,
                    () -> sessionService.findSessionsByPatientIdAndDateRange(patientId, start, end));
        }

        @Test
        @DisplayName("F-21 — Resumen mensual calcula totales correctamente")
        void resumenMensual_calculaTotales() {
            Session s1 = new Session(LocalDate.of(2025, 7, 15), LocalTime.of(7, 30),
                    1, 1.5f, 1500, 1560, null);
            s1.prePersist();
            Session s2 = new Session(LocalDate.of(2025, 7, 15), LocalTime.of(12, 0),
                    2, 1.5f, 1500, 1585, null);
            s2.prePersist();
            Session s3 = new Session(LocalDate.of(2025, 7, 15), LocalTime.of(18, 0),
                    3, 1.5f, 1500, 1550, null);
            s3.prePersist();

            when(sessionRepository.findByPatientIdAndClinicalDateBetweenOrderByClinicalDateDescDateDescHourDesc(
                    eq(patientId), any(), any())).thenReturn(List.of(s1, s2, s3));

            SessionSummaryDto summary = sessionService.summarizeSessionsByMonth(patientId, 2025, 7);

            assertEquals(3, summary.getSessionsCount());
            assertEquals(4500, summary.getTotalInfusion());      // 3 × 1500
            assertEquals(4695, summary.getTotalDrainage());       // 1560 + 1585 + 1550
            assertEquals(-195, summary.getTotalBalance());        // 4500 - 4695
        }

        @Test
        @DisplayName("Resumen diario sin sesiones retorna ceros")
        void resumenSinSesiones_retornaCeros() {
            when(sessionRepository.findByPatientIdAndClinicalDateOrderByDateDescHourDesc(
                    patientId, LocalDate.of(2025, 7, 20))).thenReturn(List.of());

            SessionSummaryDto summary = sessionService.summarizeSessionsByDay(
                    patientId, LocalDate.of(2025, 7, 20));

            assertEquals(0, summary.getSessionsCount());
            assertEquals(0, summary.getTotalInfusion());
            assertEquals(0, summary.getTotalDrainage());
            assertEquals(0, summary.getTotalBalance());
        }

        @Test
        @DisplayName("Mes inválido (13) lanza error")
        void mesInvalido_lanzaError() {
            assertThrows(IllegalArgumentException.class,
                    () -> sessionService.summarizeSessionsByMonth(patientId, 2025, 13));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════

    private SessionDto buildSessionDto(float concentration) {
        SessionDto dto = new SessionDto();
        dto.setDate(LocalDate.of(2025, 7, 15));
        dto.setHour(LocalTime.of(7, 30));
        dto.setBag(1);
        dto.setConcentration(concentration);
        dto.setInfusion(1500);
        dto.setDrainage(1560);
        dto.setObservations("test");
        return dto;
    }

    private Session buildSessionEntity(float concentration) {
        Session session = new Session(
                LocalDate.of(2025, 7, 15), LocalTime.of(7, 30),
                1, concentration, 1500, 1560, "test"
        );
        session.setPatient(testPatient);
        return session;
    }
}
