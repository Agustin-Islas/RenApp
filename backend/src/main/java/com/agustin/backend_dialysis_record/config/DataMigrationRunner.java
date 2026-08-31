package com.agustin.backend_dialysis_record.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public DataMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            int sessionsUpdated = jdbcTemplate.update("UPDATE session SET concentration = 2.3 WHERE concentration = 2.4 OR concentration = 2.40");
            if (sessionsUpdated > 0) {
                log.info("Actualizadas {} sesiones con concentración 2.4 a 2.3", sessionsUpdated);
            }

            try {
                int customConcUpdated = jdbcTemplate.update("UPDATE patient_custom_concentration SET concentration = 2.3 WHERE concentration = 2.4 OR concentration = 2.40");
                if (customConcUpdated > 0) {
                    log.info("Actualizadas {} concentraciones personalizadas de 2.4 a 2.3", customConcUpdated);
                }
            } catch (Exception e) {
                log.debug("Tabla patient_custom_concentration no requiere actualización o no existe: {}", e.getMessage());
            }

            // Limpieza de pacientes huérfanos generados por fallos en intentos fallidos de registro previos
            try {
                int orphanedPatients = jdbcTemplate.update(
                        "DELETE FROM patient WHERE id NOT IN (SELECT u.patient_id FROM user_account u WHERE u.patient_id IS NOT NULL) " +
                        "AND id NOT IN (SELECT s.patient_id FROM session s)"
                );
                if (orphanedPatients > 0) {
                    log.info("Eliminados {} registros de pacientes huérfanos (sin usuario ni sesiones asociadas)", orphanedPatients);
                }
            } catch (Exception e) {
                log.debug("Error en limpieza de pacientes huérfanos: {}", e.getMessage());
            }

            // Limpieza de doctores huérfanos generados por fallos en intentos fallidos de registro previos
            try {
                int orphanedDoctors = jdbcTemplate.update(
                        "DELETE FROM doctor WHERE id NOT IN (SELECT u.doctor_id FROM user_account u WHERE u.doctor_id IS NOT NULL) " +
                        "AND id NOT IN (SELECT p.doctor_id FROM patient p WHERE p.doctor_id IS NOT NULL)"
                );
                if (orphanedDoctors > 0) {
                    log.info("Eliminados {} registros de doctores huérfanos", orphanedDoctors);
                }
            } catch (Exception e) {
                log.debug("Error en limpieza de doctores huérfanos: {}", e.getMessage());
            }

            // Backfill de clinicalDate para ADR 0004 (PostgreSQL)
            try {
                int clinicalDateUpdated = jdbcTemplate.update(
                        "UPDATE session SET clinical_date = CASE WHEN hour < '05:00:00' THEN date - INTERVAL '1 day' ELSE date END WHERE clinical_date IS NULL"
                );
                if (clinicalDateUpdated > 0) {
                    log.info("Actualizadas {} sesiones con clinical_date (ADR 0004)", clinicalDateUpdated);
                }
            } catch (Exception e) {
                log.debug("Error en backfill de clinical_date: {}", e.getMessage());
            }

            // Fix de bolsa nocturna para el 30/06/2026 (cargada erróneamente con bolsa 1 antes del fix de cálculo de día clínico)
            try {
                int nightBagFixed = jdbcTemplate.update(
                        "UPDATE session SET bag = 3 WHERE clinical_date = '2026-06-30' AND hour < '05:00:00' AND bag = 1"
                );
                if (nightBagFixed > 0) {
                    log.info("Corregido el número de bolsa del turno trasnoche del 30/06/2026 a bolsa = 3 (modificadas: {} sesiones)", nightBagFixed);
                }
            } catch (Exception e) {
                log.debug("Error en fix de bolsa nocturna 30/06: {}", e.getMessage());
            }

            // Fase 1 de deprecación: hacer password_hash nullable y limpiar valores dummy
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE user_account ALTER COLUMN password_hash DROP NOT NULL"
                );
                int dummyCleared = jdbcTemplate.update(
                        "UPDATE user_account SET password_hash = NULL WHERE password_hash = 'dummy_hash'"
                );
                if (dummyCleared > 0) {
                    log.info("Limpiados {} registros con password_hash='dummy_hash' → NULL", dummyCleared);
                }
            } catch (Exception e) {
                log.debug("password_hash ya es nullable o no existe: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("Excepción leve durante la migración automática de datos: {}", e.getMessage());
        }
    }
}
