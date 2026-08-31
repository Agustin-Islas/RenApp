package com.agustin.backend_dialysis_record.controller;

import com.agustin.backend_dialysis_record.dto.SessionDto;
import com.agustin.backend_dialysis_record.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    @Autowired
    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<SessionDto>> getAllSession() {
        return ResponseEntity.ok(sessionService.findAll());
    }

    @PreAuthorize("@authz.canAccessSession(#sessionId)")
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionDto> getSessionById(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.findById(sessionId));
    }

    @PreAuthorize("@authz.canAccessSession(#sessionId)")
    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionDto> update(@PathVariable UUID sessionId,
                                             @Valid @RequestBody SessionDto sessionDto) {
        return ResponseEntity.ok(sessionService.update(sessionId, sessionDto));
    }

    @PreAuthorize("@authz.canAccessSession(#sessionId)")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID sessionId) {
        sessionService.delete(sessionId);
        return ResponseEntity.noContent().build();
    }
}
