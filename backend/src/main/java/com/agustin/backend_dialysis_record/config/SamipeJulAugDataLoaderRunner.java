package com.agustin.backend_dialysis_record.config;

import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.Session;
import com.agustin.backend_dialysis_record.model.auth.UserAccount;
import com.agustin.backend_dialysis_record.repository.SessionRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Carga automática de registros de diálisis peritoneal para Sathya Peña
 * correspondientes al período Julio-Agosto 2026.
 *
 * Datos originales provenientes de mensajes de WhatsApp (formato 12h),
 * pre-convertidos a formato 24h con overrides de hora aplicados.
 *
 * Reglas:
 * - Infusión por defecto: 1700 ml (salvo que se indique "Infu: XXXX")
 * - Concentración: 1.5 (amarilla) por defecto; 2.3 (verde) si el siguiente
 *   registro en orden cronológico tiene drenaje > 1900
 * - Observaciones: texto entre paréntesis (ej: "diarrea")
 * - Jornada clínica: hora antes de 05:00 pertenece al día anterior
 */
@Component
public class SamipeJulAugDataLoaderRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SamipeJulAugDataLoaderRunner.class);
    private static final String TARGET_EMAIL = "samipe374@gmail.com";

    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final JdbcTemplate jdbcTemplate;

    public SamipeJulAugDataLoaderRunner(UserAccountRepository userAccountRepository,
                                        SessionRepository sessionRepository,
                                        JdbcTemplate jdbcTemplate) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Optional<UserAccount> userOpt = userAccountRepository.findByNormalizedEmail(TARGET_EMAIL);
        if (userOpt.isEmpty() || userOpt.get().getPatient() == null) {
            log.warn("No se encontró el paciente con email {} o no tiene entidad Patient asociada.", TARGET_EMAIL);
            return;
        }

        Patient patient = userOpt.get().getPatient();
        UUID patientId = patient.getId();

        // Verificar si ya se cargaron datos de Jul-Ago 2026 (por rango de fechas)
        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session WHERE patient_id = ? AND date BETWEEN '2026-07-11' AND '2026-08-09' AND active = true",
                Integer.class, patientId);
        if (existingCount != null && existingCount > 0) {
            log.info("El paciente {} ya tiene {} sesiones en Jul-Ago 2026. Omitiendo carga.", TARGET_EMAIL, existingCount);
            return;
        }

        log.info("Iniciando importación de registros Jul-Ago 2026 para el paciente {} (ID: {})...", TARGET_EMAIL, patientId);

        // 1. Parsear datos crudos
        List<RawEntry> entries = parseRawData();

        // 2. Ordenar cronológicamente
        entries.sort(Comparator.comparing((RawEntry r) -> r.date).thenComparing(r -> r.hour));

        // 3. Asignar concentración: verde (2.3) si el SIGUIENTE registro tiene drenaje > 1900
        for (int i = 0; i < entries.size(); i++) {
            boolean nextHighDrainage = (i + 1 < entries.size()) && entries.get(i + 1).drainage > 1900;
            entries.get(i).concentration = nextHighDrainage ? 2.3f : 1.5f;
        }

        // 4. Agrupar por jornada clínica para numerar bolsas
        Map<LocalDate, List<RawEntry>> byClinicalDate = new LinkedHashMap<>();
        for (RawEntry e : entries) {
            LocalDate clinicalDate = e.hour.isBefore(LocalTime.of(5, 0)) ? e.date.minusDays(1) : e.date;
            byClinicalDate.computeIfAbsent(clinicalDate, k -> new ArrayList<>()).add(e);
        }

        // 5. Crear sesiones
        List<Session> sessionsToSave = new ArrayList<>();

        for (Map.Entry<LocalDate, List<RawEntry>> entry : byClinicalDate.entrySet()) {
            List<RawEntry> dayEntries = entry.getValue();
            for (int i = 0; i < dayEntries.size(); i++) {
                RawEntry raw = dayEntries.get(i);
                Session session = new Session();
                session.setPatient(patient);
                session.setDate(raw.date);
                session.setHour(raw.hour);
                session.setBag(i + 1);
                session.setConcentration(raw.concentration);
                session.setInfusion(raw.infusion);
                session.setDrainage(raw.drainage);
                session.setObservations(raw.observations);
                sessionsToSave.add(session);
            }
        }

        sessionRepository.saveAll(sessionsToSave);
        log.info("✅ Carga Jul-Ago 2026 completada: {} sesiones insertadas para {}.", sessionsToSave.size(), TARGET_EMAIL);
    }

    /**
     * Parsea los datos crudos de WhatsApp pre-convertidos a formato 24h.
     * Formato de línea: [día/mes, HH:MM] Sayi: drenaje (observación opcional)
     * Líneas "Infu: XXXX" modifican la infusión del registro anterior.
     */
    private List<RawEntry> parseRawData() {
        // Horas ya convertidas a 24h. Overrides de hora (paréntesis numéricos) ya aplicados.
        // Solo queda "diarrea" como observación en paréntesis.
        String rawText = """
[11/7, 12:00] Sayi: 1740
[11/7, 17:45] Sayi: 1940
[11/7, 20:47] Sayi: 1855
[12/7, 01:41] Sayi: 1815
[12/7, 12:23] Sayi: 1775
[12/7, 18:24] Sayi: 1980
[12/7, 23:42] Sayi: 1835
[13/7, 07:14] Sayi: 1840
[13/7, 14:31] Sayi: 1910
Infu: 1570
[13/7, 19:32] Sayi: 1615
Infu: 1620
[14/7, 01:01] Sayi: 1755
[14/7, 11:48] Sayi: 1030
Infu: 1200
[14/7, 16:43] Sayi: 2040
[15/7, 00:22] Sayi: 1805
[15/7, 11:50] Sayi: 1720
[15/7, 14:24] Sayi: 1940
[15/7, 21:21] Sayi: 1833
[15/7, 23:30] Sayi: 1735
[16/7, 07:14] Sayi: 1710 (diarrea)
[16/7, 14:30] Sayi: 2040
[16/7, 19:28] Sayi: 1830
[16/7, 23:41] Sayi: 1840
[17/7, 07:16] Sayi: 1850
[17/7, 14:12] Sayi: 2005
[17/7, 18:13] Sayi: 1775
[18/7, 00:15] Sayi: 1805
[18/7, 10:48] Sayi: 1825
[18/7, 14:25] Sayi: 1980
[18/7, 19:46] Sayi: 1820
[19/7, 00:14] Sayi: 1750
[19/7, 11:47] Sayi: 1725
[19/7, 13:46] Sayi: 1885
[19/7, 20:51] Sayi: 1780
[20/7, 00:51] Sayi: 1805
[20/7, 08:42] Sayi: 1825
[20/7, 14:12] Sayi: 1975
[20/7, 20:29] Sayi: 1730
[21/7, 01:09] Sayi: 1840
[21/7, 11:29] Sayi: 1784
[21/7, 15:15] Sayi: 1980
[22/7, 00:17] Sayi: 1740
[22/7, 10:30] Sayi: 1770
[22/7, 16:02] Sayi: 2040
[23/7, 00:15] Sayi: 1730
[23/7, 10:40] Sayi: 1745
[23/7, 14:20] Sayi: 2000
[23/7, 19:26] Sayi: 1800
[24/7, 00:33] Sayi: 1730
[24/7, 11:14] Sayi: 1800
[24/7, 17:18] Sayi: 2000
[24/7, 23:54] Sayi: 1755
[25/7, 11:00] Sayi: 1755
[25/7, 14:52] Sayi: 1965
[25/7, 20:13] Sayi: 1750
[26/7, 00:41] Sayi: 1810
[26/7, 11:46] Sayi: 1840
[26/7, 16:14] Sayi: 1775
[26/7, 21:12] Sayi: 2015
[27/7, 01:09] Sayi: 1805
[27/7, 11:26] Sayi: 1225
Infu: 1500
[27/7, 16:28] Sayi: 2205
[28/7, 00:11] Sayi: 1780
[28/7, 08:48] Sayi: 1840
[28/7, 16:39] Sayi: 1745
[28/7, 20:23] Sayi: 1985
[29/7, 00:10] Sayi: 1800
[29/7, 07:11] Sayi: 1735
[29/7, 16:46] Sayi: 1765
[29/7, 20:27] Sayi: 1800
[29/7, 23:41] Sayi: 1935
[30/7, 07:11] Sayi: 1775
[30/7, 13:00] Sayi: 2050
[30/7, 19:17] Sayi: 1750
[30/7, 23:55] Sayi: 1735
[31/7, 07:13] Sayi: 1890
[31/7, 16:41] Sayi: 1775
[1/8, 00:11] Sayi: 1945
[1/8, 10:06] Sayi: 1155
Infu: 1500
[1/8, 15:13] Sayi: 2410
[2/8, 00:35] Sayi: 1715
[2/8, 11:27] Sayi: 1790
[2/8, 15:27] Sayi: 1940
[2/8, 19:39] Sayi: 1760
[2/8, 23:27] Sayi: 1830
[3/8, 06:40] Sayi: 1800
[3/8, 13:04] Sayi: 2030
[3/8, 19:18] Sayi: 1750
[3/8, 23:16] Sayi: 1845
[4/8, 09:52] Sayi: 1080
Infu: 1500
[4/8, 13:09] Sayi: 2365
[4/8, 18:20] Sayi: 1620
[5/8, 00:14] Sayi: 1975
[5/8, 09:54] Sayi: 1815
[5/8, 12:26] Sayi: 1770
[5/8, 18:18] Sayi: 2040
[5/8, 23:27] Sayi: 1730
[6/8, 07:15] Sayi: 1840
[6/8, 14:45] Sayi: 2015
[6/8, 19:26] Sayi: 1805
[7/8, 00:00] Sayi: 1830
[7/8, 07:16] Sayi: 1825
[7/8, 14:34] Sayi: 2010
[7/8, 19:08] Sayi: 1805
[8/8, 00:23] Sayi: 1810
[8/8, 10:35] Sayi: 1860
[8/8, 15:29] Sayi: 1995
[9/8, 00:12] Sayi: 1800
[9/8, 10:46] Sayi: 1790
[9/8, 16:18] Sayi: 2020
[9/8, 20:10] Sayi: 1805
""";

        List<RawEntry> list = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[")) {
                try {
                    int closeBracket = line.indexOf("]");
                    String dateTimePart = line.substring(1, closeBracket); // "11/7, 12:00"
                    String[] dtParts = dateTimePart.split(",");
                    String datePart = dtParts[0].trim(); // "11/7"
                    String timePart = dtParts[1].trim(); // "12:00"

                    String[] dayMonth = datePart.split("/");
                    int day = Integer.parseInt(dayMonth[0].trim());
                    int month = Integer.parseInt(dayMonth[1].trim());

                    String[] timeTokens = timePart.split(":");
                    int hour = Integer.parseInt(timeTokens[0].trim());
                    int minute = Integer.parseInt(timeTokens[1].trim());

                    // Extraer drenaje y observaciones
                    String afterColon = line.substring(line.lastIndexOf(":") + 1).trim();
                    String obs = null;
                    int parenStart = afterColon.indexOf('(');
                    if (parenStart >= 0) {
                        int parenEnd = afterColon.indexOf(')', parenStart);
                        if (parenEnd >= 0) {
                            obs = afterColon.substring(parenStart + 1, parenEnd).trim();
                        }
                        afterColon = afterColon.substring(0, parenStart).trim();
                    }
                    // Eliminar caracteres no numéricos (ej: "1805b" → "1805")
                    String numStr = afterColon.replaceAll("[^0-9]", "");
                    int drainage = Integer.parseInt(numStr);

                    list.add(new RawEntry(LocalDate.of(2026, month, day), LocalTime.of(hour, minute), 1700, drainage, obs));
                } catch (Exception e) {
                    log.error("Error parseando línea de registro: {}", line, e);
                }
            } else if (!list.isEmpty() && line.toLowerCase().contains("infu:")) {
                try {
                    String numStr = line.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        int customInfu = Integer.parseInt(numStr);
                        list.get(list.size() - 1).infusion = customInfu;
                    }
                } catch (Exception e) {
                    log.error("Error parseando infusión en línea: {}", line, e);
                }
            }
        }
        return list;
    }

    private static class RawEntry {
        LocalDate date;
        LocalTime hour;
        int infusion;
        int drainage;
        String observations;
        float concentration;

        RawEntry(LocalDate date, LocalTime hour, int infusion, int drainage, String observations) {
            this.date = date;
            this.hour = hour;
            this.infusion = infusion;
            this.drainage = drainage;
            this.observations = observations;
        }
    }
}
