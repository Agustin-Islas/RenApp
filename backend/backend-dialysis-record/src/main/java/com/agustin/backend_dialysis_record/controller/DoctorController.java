package com.agustin.backend_dialysis_record.controller;

import com.agustin.backend_dialysis_record.dto.DoctorDto;
import com.agustin.backend_dialysis_record.dto.DoctorMeDto;
import com.agustin.backend_dialysis_record.dto.PatientDto;
import com.agustin.backend_dialysis_record.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    @Autowired
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /* =====================================================
       /me (Self endpoints)
       ===================================================== */

    @GetMapping("/me")
    public ResponseEntity<DoctorMeDto> getMe(Authentication auth) {
        UUID authId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(doctorService.getMyDoctor(authId));
    }

    @GetMapping("/me/patients")
    public ResponseEntity<List<PatientDto>> getMyPatients(Authentication auth) {
        UUID authId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(doctorService.getMyPatients(authId));
    }

    @PostMapping("/me/patients/{patientId}")
    public ResponseEntity<PatientDto> addPatientToMe(Authentication auth, @PathVariable UUID patientId) {
        UUID authId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(doctorService.addPatientToMyDoctor(authId, patientId));
    }

    @DeleteMapping("/me/patients/{patientId}")
    public ResponseEntity<Void> removePatientFromMe(Authentication auth, @PathVariable UUID patientId) {
        UUID authId = UUID.fromString(auth.getName());
        doctorService.removePatientFromMyDoctor(authId, patientId);
        return ResponseEntity.noContent().build();
    }

    /* =====================================================
       Admin/Managed endpoints (by doctorId)
       ===================================================== */

    @PreAuthorize("@authz.canAccessDoctor(#doctorId)")
    @PatchMapping("/{doctorId}/activate")
    public ResponseEntity<DoctorDto> activateDoctor(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(doctorService.activate(doctorId));
    }

    @PreAuthorize("@authz.canAccessDoctor(#doctorId)")
    @PostMapping("/{doctorId}/patients/{patientId}")
    public ResponseEntity<PatientDto> addPatient(@PathVariable UUID doctorId, @PathVariable UUID patientId) {
        return ResponseEntity.ok(doctorService.addPatientToDoctor(doctorId, patientId));
    }

    @PreAuthorize("@authz.canAccessDoctor(#doctorId)")
    @GetMapping("/{doctorId}/patients")
    public ResponseEntity<List<PatientDto>> getPatientsByDoctor(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(doctorService.getPatientsByDoctor(doctorId));
    }

    @PreAuthorize("@authz.canAccessDoctor(#doctorId)")
    @DeleteMapping("/{doctorId}/patients/{patientId}")
    public ResponseEntity<Void> removePatient(@PathVariable UUID doctorId, @PathVariable UUID patientId) {
        doctorService.removePatientFromDoctor(doctorId, patientId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.isAdmin()")
    @PostMapping
    public ResponseEntity<DoctorDto> createDoctor(@Valid @RequestBody DoctorDto doctorDto) {
        return ResponseEntity.ok(doctorService.create(doctorDto));
    }

    @PreAuthorize("@authz.isAdmin()")
    @GetMapping
    public ResponseEntity<List<DoctorDto>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.findAll());
    }

    @PreAuthorize("@authz.canAccessDoctor(#doctorId)")
    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorDto> getDoctorById(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(doctorService.findById(doctorId));
    }

    @PreAuthorize("@authz.canAccessDoctor(#doctorId)")
    @PutMapping("/{doctorId}")
    public ResponseEntity<DoctorDto> updateDoctor(@PathVariable UUID doctorId,
                                                  @Valid @RequestBody DoctorDto doctorDto) {
        return ResponseEntity.ok(doctorService.update(doctorId, doctorDto));
    }

    @PreAuthorize("@authz.isAdmin()")
    @DeleteMapping("/{doctorId}")
    public ResponseEntity<Void> deleteDoctorById(@PathVariable UUID doctorId) {
        doctorService.delete(doctorId);
        return ResponseEntity.noContent().build();
    }
}
