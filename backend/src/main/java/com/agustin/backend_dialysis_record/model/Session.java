package com.agustin.backend_dialysis_record.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE session SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Session {
    public static final LocalTime CLINICAL_CUTOFF = LocalTime.of(5, 0);

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDate date;
    private LocalTime hour;
    
    @Column(name = "clinical_date")
    private LocalDate clinicalDate;

    private int bag;
    private float concentration;
    private int infusion;
    private int drainage;
    private int partial;
    private String observations;
    
    @Column(name = "severity_level")
    private Integer severityLevel;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    public Session(LocalDate date, LocalTime hour, int bag, float concentration,
                   int infusion, int drainage, String observations) {
        this.date = date;
        this.hour = hour;
        this.bag = bag;
        this.concentration = concentration;
        this.infusion = infusion;
        this.drainage = drainage;
        this.observations = observations;
    }

    public LocalDate getClinicalDate() {
        if (clinicalDate == null && date != null) {
            computeClinicalDate();
        }
        return clinicalDate != null ? clinicalDate : date;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        computePartial();
        computeClinicalDate();
        computeSeverityLevel();
    }

    private void computePartial() {
        this.partial = this.infusion - this.drainage;
    }

    public void computeClinicalDate() {
        if (this.date != null && this.hour != null && this.hour.isBefore(CLINICAL_CUTOFF)) {
            this.clinicalDate = this.date.minusDays(1);
        } else if (this.date != null) {
            this.clinicalDate = this.date;
        }
    }
    
    private void computeSeverityLevel() {
        if (this.observations == null || this.observations.trim().isEmpty()) {
            this.severityLevel = 3;
            return;
        }
        
        String obs = this.observations.toLowerCase();
        
        // Nivel 1: Crítico / Infección (Rojo)
        String[] level1Keywords = {"turbio", "sangre", "rojo", "fiebre", "pus", "caliente", "infeccion", "sucio", "niebla", "supura"};
        for (String keyword : level1Keywords) {
            if (obs.contains(keyword)) {
                this.severityLevel = 1;
                return;
            }
        }
        
        // Nivel 2: Mecánico / Advertencia (Amarillo)
        String[] level2Keywords = {"dolor", "duele", "mareo", "lento", "tapado", "fuga", "molestia", "tirón", "espeso", "obstruido"};
        for (String keyword : level2Keywords) {
            if (obs.contains(keyword)) {
                this.severityLevel = 2;
                return;
            }
        }
        
        // Nivel 3: Informativo (Azul/Gris)
        this.severityLevel = 3;
    }

    @PreUpdate
    public void preUpdate() {
        computePartial();
        computeClinicalDate();
        computeSeverityLevel();
    }

}
