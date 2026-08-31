package com.agustin.backend_dialysis_record.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class LinkAudit {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id")
    private PatientInvitation invitation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkAuditAction action;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
