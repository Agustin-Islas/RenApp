package com.agustin.backend_dialysis_record.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE doctor SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Doctor {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    
    @Column(name = "clinic_id")
    private UUID clinicId;

    @Column(nullable = false)
    private boolean active = true;
    private String name;
    private String surname;



    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
