package com.agustin.backend_dialysis_record.controller;

import com.agustin.backend_dialysis_record.dto.PatientDto;
import com.agustin.backend_dialysis_record.dto.PatientMeDto;
import com.agustin.backend_dialysis_record.dto.SessionDto;
import com.agustin.backend_dialysis_record.dto.SessionSummaryDto;
import com.agustin.backend_dialysis_record.service.PatientService;
import com.agustin.backend_dialysis_record.service.SessionService;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final SessionService sessionService;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    public PatientController(PatientService patientService, SessionService sessionService, UserAccountRepository userAccountRepository) {
        this.patientService = patientService;
        this.sessionService = sessionService;
        this.userAccountRepository = userAccountRepository;
    }

    /* =====================================================
       PATIENTS
       ===================================================== */

    @PreAuthorize("@authz.isDoctorOrAdmin()")
    @PostMapping
    public ResponseEntity<PatientDto> create(Authentication auth, @Valid @RequestBody PatientDto patientDto) {
        UUID authId = UUID.fromString(auth.getName());
        UUID userAccountId = userAccountRepository.findByAuthId(authId).orElseThrow().getId();
        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(userAccountId).orElse(null); // admin might be null
        
        PatientDto patient = patientService.create(patientDto, doctorId);
        return ResponseEntity.ok(patient);
    }

    @GetMapping("/me")
    public ResponseEntity<PatientMeDto> getMe(Authentication auth) {
        UUID authId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(patientService.getMyPatient(authId));
    }

    // Listar pacientes: solo doctores (filtra por vinculación M-N)
    @PreAuthorize("@authz.isDoctorOrAdmin()")
    @GetMapping
    public ResponseEntity<List<PatientDto>> getAllPatients(Authentication auth) {
        UUID authId = UUID.fromString(auth.getName());
        UUID userAccountId = userAccountRepository.findByAuthId(authId).orElseThrow().getId();
        
        // TODO: si es ADMIN debería ver todos, pero ahora asume rol Doctor
        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(userAccountId).orElseThrow();
        
        List<PatientDto> patients = patientService.findPatientsByDoctor(doctorId);
        return ResponseEntity.ok(patients);
    }

    // Ver paciente: doctor dueño o el propio paciente
    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @GetMapping("/{patientId}")
    public ResponseEntity<PatientDto> getPatientById(@PathVariable UUID patientId) {
        PatientDto patient = patientService.findById(patientId);
        return ResponseEntity.ok(patient);
    }

    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @PutMapping("/{patientId}")
    public ResponseEntity<PatientDto> update(@PathVariable UUID patientId,
                                             @Valid @RequestBody PatientDto patientDto) {
        PatientDto patient = patientService.update(patientId, patientDto);
        return ResponseEntity.ok(patient);
    }

    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId) {
        patientService.delete(patientId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @PatchMapping("/{patientId}/activate")
    public ResponseEntity<PatientDto> activatePatient(@PathVariable UUID patientId) {
        PatientDto patientDto = patientService.activate(patientId);
        return ResponseEntity.ok(patientDto);
    }

    /* =====================================================
       SESSIONS UNDER PATIENT
       ===================================================== */

    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @PostMapping("/{patientId}/sessions")
    public ResponseEntity<SessionDto> createSession(@PathVariable UUID patientId,
                                                    @Valid @RequestBody SessionDto sessionDto) {
        SessionDto created = sessionService.createForPatient(patientId, sessionDto);
        return ResponseEntity.ok(created);
    }

    // Listar sesiones del paciente (con rango opcional)
    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @GetMapping("/{patientId}/sessions")
    public ResponseEntity<List<SessionDto>> getSessions(@PathVariable UUID patientId,
                                                        @RequestParam(required = false) LocalDate startDate,
                                                        @RequestParam(required = false) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(
                    sessionService.findSessionsByPatientIdAndDateRange(patientId, startDate, endDate)
            );
        }
        return ResponseEntity.ok(sessionService.findSessionsByPatientId(patientId));
    }

    // Sesiones por día
    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @GetMapping("/{patientId}/sessions/day/{day}")
    public ResponseEntity<List<SessionDto>> getSessionsByDay(@PathVariable UUID patientId,
                                                             @PathVariable LocalDate day) {
        return ResponseEntity.ok(sessionService.findSessionsByDay(patientId, day));
    }

    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @GetMapping("/{patientId}/sessions/summary/day/{day}")
    public ResponseEntity<SessionSummaryDto> getSessionSummaryByDay(@PathVariable UUID patientId,
                                                                    @PathVariable LocalDate day) {
        return ResponseEntity.ok(sessionService.summarizeSessionsByDay(patientId, day));
    }

    @PreAuthorize("@authz.canAccessPatient(#patientId)")
    @GetMapping("/{patientId}/sessions/summary/month")
    public ResponseEntity<SessionSummaryDto> getSessionSummaryByMonth(@PathVariable UUID patientId,
                                                                      @RequestParam int year,
                                                                      @RequestParam int month) {
        return ResponseEntity.ok(sessionService.summarizeSessionsByMonth(patientId, year, month));
    }
}
