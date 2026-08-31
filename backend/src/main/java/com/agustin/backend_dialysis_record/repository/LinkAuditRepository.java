package com.agustin.backend_dialysis_record.repository;

import com.agustin.backend_dialysis_record.model.LinkAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LinkAuditRepository extends JpaRepository<LinkAudit, UUID> {
}
