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

@Component
public class SamipeDataLoaderRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SamipeDataLoaderRunner.class);
    private static final String TARGET_EMAIL = "samipe374@gmail.com";
    private static final String MARKER_OBSERVATION = "Carga automatica Sathya🌸";

    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final JdbcTemplate jdbcTemplate;

    public SamipeDataLoaderRunner(UserAccountRepository userAccountRepository,
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

        // Verificar si ya se realizó la carga anteriormente
        List<Session> existingSessions = sessionRepository.findByPatientIdOrderByDateDesc(patientId);
        boolean alreadyLoaded = existingSessions.stream()
                .anyMatch(s -> MARKER_OBSERVATION.equals(s.getObservations()));
        if (alreadyLoaded) {
            log.info("El paciente {} ya tiene cargados los registros automáticos de Sathya🌸 ({} registros existentes). Omitiendo nueva carga.", TARGET_EMAIL, existingSessions.size());
            return;
        }

        log.info("Iniciando borrado e importación de 70 registros para el paciente {} (ID: {})...", TARGET_EMAIL, patientId);

        // Borrar registros actuales en BD (hard delete para limpieza absoluta)
        int deleted = jdbcTemplate.update("DELETE FROM session WHERE patient_id = ?", patientId);
        log.info("Eliminados {} registros antiguos del paciente.", deleted);

        // Parsear datos en crudo
        List<RawEntry> entries = parseRawData();

        // Calcular clinical date y agrupar
        Map<LocalDate, List<RawEntry>> byClinicalDate = new HashMap<>();
        for (RawEntry e : entries) {
            LocalDate clinicalDate = e.hour.isBefore(LocalTime.of(5, 0)) ? e.date.minusDays(1) : e.date;
            byClinicalDate.computeIfAbsent(clinicalDate, k -> new ArrayList<>()).add(e);
        }

        List<Session> sessionsToSave = new ArrayList<>();

        // Ordenar cada jornada clínica y asignar bolsa, concentración, infusión y drenaje
        for (Map.Entry<LocalDate, List<RawEntry>> entry : byClinicalDate.entrySet()) {
            List<RawEntry> dayEntries = entry.getValue();
            dayEntries.sort(Comparator.comparing((RawEntry r) -> r.date).thenComparing(r -> r.hour));

            for (int i = 0; i < dayEntries.size(); i++) {
                RawEntry raw = dayEntries.get(i);
                Session session = new Session();
                session.setPatient(patient);
                session.setDate(raw.date);
                session.setHour(raw.hour);
                session.setBag(i + 1);
                session.setConcentration(i == 0 ? 2.3f : 1.5f); // Verde (2.3) primer cambio, Amarillo (1.5) el resto
                session.setInfusion(raw.infusion);
                session.setDrainage(raw.drainage);
                session.setObservations(MARKER_OBSERVATION);
                sessionsToSave.add(session);
            }
        }

        sessionRepository.saveAll(sessionsToSave);
        log.info("✅ Carga completada con éxito: Se insertaron {} sesiones para {}.", sessionsToSave.size(), TARGET_EMAIL);
    }

    private List<RawEntry> parseRawData() {
        String rawText = """
[1/6, 14:19] Sathya🌸: 2005
[1/6, 18:55] Sathya🌸: 1815
[2/6, 00:10] Sathya🌸: 1805
[2/6, 10:55] Sathya🌸: 1800
[2/6, 14:21] Sathya🌸: 1945
[2/6, 17:35] Sathya🌸: 1760
[2/6, 23:59] Sathya🌸: 1780
[3/6, 07:14] Sathya🌸: 1820
[3/6, 13:45] Sathya🌸: 1205
Infu: 1500
[3/6, 17:59] Sathya🌸: 2350
[3/6, 23:35] Sathya🌸: 1820
[4/6, 07:15] Sathya🌸: 1760
[4/6, 13:24] Sathya🌸: 2070
[4/6, 18:51] Sathya🌸: 1805
[4/6, 23:58] Sathya🌸: 1770
[5/6, 07:16] Sathya🌸: 1846
[5/6, 13:00] Sathya🌸: 2035
[6/6, 00:15] Sathya🌸: 1675
[6/6, 10:32] Sathya🌸: 1815
[6/6, 13:40] Sathya🌸: 1925
[6/6, 17:40] Sathya🌸: 1830
[7/6, 01:06] Sathya🌸: 1700
[7/6, 10:15] Sathya🌸: 1745
[7/6, 14:24] Sathya🌸: 1965
Infu: 1655
[8/6, 00:42] Sathya🌸: 1665
[8/6, 09:21] Sathya🌸: 1840
[8/6, 14:47] Sathya🌸: 2010
[8/6, 21:24] Sathya🌸: 1845
[8/6, 23:33] Sathya🌸: 1755
[9/6, 07:16] Sathya🌸: 1800
[9/6, 13:41] Sathya🌸: 2115
[9/6, 20:36] Sathya🌸: 1740
[9/6, 23:04] Sathya🌸: 1745
[10/6, 06:29] Sathya🌸: 1785
[10/6, 13:35] Sathya🌸: 2135
[10/6, 20:36] Sathya🌸: 1800
[10/6, 23:41] Sathya🌸: 1800
[11/6, 10:21] Sathya🌸: 1750
[11/6, 13:43] Sathya🌸: 2000
[12/6, 00:14] Sathya🌸: 1780
[12/6, 06:54] Sathya🌸: 1865
[12/6, 13:36] Sathya🌸: 2015
[12/6, 17:31] Sathya🌸: 1850
[12/6, 23:33] Sathya🌸: 1715
[13/6, 09:20] Sathya🌸: 1820
[13/6, 14:23] Sathya🌸: 2000
[13/6, 18:22] Sathya🌸: 1870
[13/6, 23:46] Sathya🌸: 1800
[14/6, 14:29] Sathya🌸: 2060
[15/6, 00:02] Sathya🌸: 1765
[15/6, 10:16] Sathya🌸: 1445
[15/6, 14:46] Sathya🌸: 2295
Infu: 1630
[15/6, 17:50] Sathya🌸: 1750
[15/6, 23:15] Sathya🌸: 1785
[16/6, 07:17] Sathya🌸: 1855
[16/6, 13:30] Sathya🌸: 2010
[16/6, 23:50] Sathya🌸: 1770
[17/6, 07:10] Sathya🌸: 1850
[17/6, 13:35] Sathya🌸: 1970
[17/6, 18:09] Sathya🌸: 1805
[17/6, 23:47] Sathya🌸: 1825
[18/6, 07:13] Sathya🌸: 1825
[18/6, 14:32] Sathya🌸: 2045
[18/6, 19:38] Sathya🌸: 1480
Infu: 1500
[18/6, 23:39] Sathya🌸: 1880
[19/6, 07:16] Sathya🌸: 1835
[19/6, 13:41] Sathya🌸: 1915
[19/6, 18:19] Sathya🌸: 1850
[20/6, 00:12] Sathya🌸: 1820
[20/6, 10:37] Sathya🌸: 1605
[20/6, 16:06] Sathya🌸: 2210
[21/6, 00:11] Sathya🌸: 1745
[21/6, 10:12] Sathya🌸: 1815
[21/6, 18:24] Sathya🌸: 1740
[22/6, 00:28] Sathya🌸: 1825
[22/6, 10:25] Sathya🌸: 1825
[22/6, 13:37] Sathya🌸: 1905
[22/6, 18:07] Sathya🌸: 1860
[22/6, 23:21] Sathya🌸: 1835
[23/6, 07:15] Sathya🌸: 1720
[23/6, 13:29] Sathya🌸: 2045
[23/6, 18:47] Sathya🌸: 1820
[23/6, 23:53] Sathya🌸: 1805
[24/6, 18:20] Sathya🌸: 1880
[25/6, 11:54] Sathya🌸: 1770
1630 insi
[25/6, 14:59] Sathya🌸: 1405
Infu: 1500
[25/6, 23:22] Sathya🌸: 1880
[26/6, 00:30] Sathya🌸: 1530 
Infu: 1500
[26/6, 10:21] Sathya🌸: 1715
[26/6, 16:13] Sathya🌸: 2015
[27/6, 00:22] Sathya🌸: 1765
[27/6, 10:53] Sathya🌸: 1785
[27/6, 16:33] Sathya🌸: 2035
[27/6, 20:49] Sathya🌸: 1370
Infu: 1500
[28/6, 01:01] Sathya🌸: 2005
[28/6, 11:41] Sathya🌸: 1765
[28/6, 16:33] Sathya🌸: 2000
[29/6, 00:13] Sathya🌸: 1840
[29/6, 07:15] Sathya🌸: 1800
[29/6, 13:30] Sathya🌸: 2000
[29/6, 18:18] Sathya🌸: 1810
[30/6, 00:04] Sathya🌸: 1825
[30/6, 11:48] Sathya🌸: 1740
[30/6, 17:16] Sathya🌸: 1980
""";

        List<RawEntry> list = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[")) {
                try {
                    int closeBracket = line.indexOf("]");
                    String dateTimePart = line.substring(1, closeBracket); // "1/6, 14:19"
                    String[] dtParts = dateTimePart.split(",");
                    String datePart = dtParts[0].trim(); // "1/6"
                    String timePart = dtParts[1].trim(); // "14:19"

                    int day = Integer.parseInt(datePart.split("/")[0].trim());
                    String[] timeTokens = timePart.split(":");
                    int hour = Integer.parseInt(timeTokens[0].trim());
                    int minute = Integer.parseInt(timeTokens[1].trim());

                    String afterColon = line.substring(line.lastIndexOf(":") + 1).trim();
                    int drainage = Integer.parseInt(afterColon);

                    list.add(new RawEntry(LocalDate.of(2026, 6, day), LocalTime.of(hour, minute), 1700, drainage));
                } catch (Exception e) {
                    log.error("Error parseando línea de registro: {}", line, e);
                }
            } else if (!list.isEmpty() && (line.toLowerCase().contains("infu:") || line.toLowerCase().contains("insi"))) {
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

        RawEntry(LocalDate date, LocalTime hour, int infusion, int drainage) {
            this.date = date;
            this.hour = hour;
            this.infusion = infusion;
            this.drainage = drainage;
        }
    }
}
