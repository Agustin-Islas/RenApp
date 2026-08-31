package com.agustin.backend_dialysis_record.controller;

import com.agustin.backend_dialysis_record.dto.CreateInvitationDto;
import com.agustin.backend_dialysis_record.dto.InvitationDto;
import com.agustin.backend_dialysis_record.service.InvitationService;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final UserAccountRepository userAccountRepository;

    public InvitationController(InvitationService invitationService, UserAccountRepository userAccountRepository) {
        this.invitationService = invitationService;
        this.userAccountRepository = userAccountRepository;
    }

    @PreAuthorize("@authz.isDoctorOrAdmin()")
    @PostMapping
    public ResponseEntity<InvitationDto> createInvitation(Authentication auth, @RequestBody CreateInvitationDto dto) {
        UUID authId = UUID.fromString(auth.getName());
        UUID userAccountId = userAccountRepository.findByAuthId(authId).orElseThrow().getId();
        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(userAccountId).orElseThrow();
        return ResponseEntity.ok(invitationService.createInvitation(doctorId, dto));
    }

    @PreAuthorize("@authz.isDoctorOrAdmin()")
    @GetMapping("/doctor/me")
    public ResponseEntity<List<InvitationDto>> getMyDoctorInvitations(Authentication auth) {
        UUID authId = UUID.fromString(auth.getName());
        UUID userAccountId = userAccountRepository.findByAuthId(authId).orElseThrow().getId();
        UUID doctorId = userAccountRepository.findDoctorIdByUserAccountId(userAccountId).orElseThrow();
        return ResponseEntity.ok(invitationService.getPendingInvitationsForDoctor(doctorId));
    }

    @PreAuthorize("@authz.isPatient()")
    @GetMapping("/patient/me")
    public ResponseEntity<List<InvitationDto>> getMyPatientInvitations(Authentication auth,
                                                                       @RequestParam(required = false) String email,
                                                                       @RequestParam(required = false) Integer dni) {
        UUID authId = UUID.fromString(auth.getName());
        UUID userAccountId = userAccountRepository.findByAuthId(authId).orElseThrow().getId();
        UUID patientId = userAccountRepository.findPatientIdByUserAccountId(userAccountId).orElseThrow();
        return ResponseEntity.ok(invitationService.getInvitationsForPatient(patientId, email, dni));
    }

    @PreAuthorize("@authz.isPatient()")
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<InvitationDto> acceptInvitation(@PathVariable UUID invitationId, Authentication auth) {
        UUID authId = UUID.fromString(auth.getName());
        UUID userAccountId = userAccountRepository.findByAuthId(authId).orElseThrow().getId();
        UUID patientId = userAccountRepository.findPatientIdByUserAccountId(userAccountId).orElseThrow();
        return ResponseEntity.ok(invitationService.acceptInvitation(invitationId, patientId));
    }

    @PreAuthorize("@authz.isPatient()")
    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<InvitationDto> rejectInvitation(@PathVariable UUID invitationId, Authentication auth) {
        UUID authId = UUID.fromString(auth.getName());
        UUID userAccountId = userAccountRepository.findByAuthId(authId).orElseThrow().getId();
        UUID patientId = userAccountRepository.findPatientIdByUserAccountId(userAccountId).orElseThrow();
        return ResponseEntity.ok(invitationService.rejectInvitation(invitationId, patientId));
    }

    @PostMapping("/{invitationId}/revoke")
    public ResponseEntity<Void> revokeAccess(@PathVariable UUID invitationId, Authentication auth) {
        // ... (Para revocar necesitamos doctorId y patientId. El endpoint puede cambiar según quién revoca).
        return ResponseEntity.ok().build();
    }
}
