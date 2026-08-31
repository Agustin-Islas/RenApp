package com.agustin.backend_dialysis_record.service.impl;

import com.agustin.backend_dialysis_record.dto.CreateInvitationDto;
import com.agustin.backend_dialysis_record.dto.InvitationDto;
import com.agustin.backend_dialysis_record.model.*;
import com.agustin.backend_dialysis_record.repository.*;
import com.agustin.backend_dialysis_record.service.InvitationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class InvitationServiceImpl implements InvitationService {

    private final PatientInvitationRepository invitationRepo;
    private final DoctorPatientAccessRepository accessRepo;
    private final LinkAuditRepository auditRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final UserAccountRepository userAccountRepo;

    public InvitationServiceImpl(PatientInvitationRepository invitationRepo,
                                 DoctorPatientAccessRepository accessRepo,
                                 LinkAuditRepository auditRepo,
                                 DoctorRepository doctorRepo,
                                 PatientRepository patientRepo,
                                 UserAccountRepository userAccountRepo) {
        this.invitationRepo = invitationRepo;
        this.accessRepo = accessRepo;
        this.auditRepo = auditRepo;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.userAccountRepo = userAccountRepo;
    }

    @Override
    @Transactional
    public InvitationDto createInvitation(UUID doctorId, CreateInvitationDto dto) {
        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        Patient patient = null;
        if (dto.getPatientEmail() != null) {
            var uaOpt = userAccountRepo.findByNormalizedEmail(dto.getPatientEmail());
            if (uaOpt.isPresent() && uaOpt.get().getPatient() != null) {
                patient = uaOpt.get().getPatient();
                if (accessRepo.existsByDoctorIdAndPatientId(doctorId, patient.getId())) {
                    throw new IllegalArgumentException("El paciente ya se encuentra vinculado a este médico");
                }
            }
        }

        boolean hasPending = invitationRepo.findByDoctorId(doctorId).stream()
                .anyMatch(i -> i.getStatus() == InvitationStatus.PENDING &&
                        ( (i.getPatientEmail() != null && i.getPatientEmail().equalsIgnoreCase(dto.getPatientEmail())) ||
                          (i.getPatientDni() != null && i.getPatientDni().equals(dto.getPatientDni())) ));

        if (hasPending) {
            throw new IllegalArgumentException("Ya existe una invitación pendiente para este paciente");
        }

        PatientInvitation inv = new PatientInvitation();
        inv.setDoctor(doctor);
        inv.setPatientDni(dto.getPatientDni());
        inv.setPatientEmail(dto.getPatientEmail());
        
        if (patient != null) {
            inv.setPatient(patient);
        }

        invitationRepo.save(inv);

        logAudit(inv, LinkAuditAction.INVITATION_CREATED, doctorId);

        return toDto(inv);
    }

    @Override
    @Transactional
    public InvitationDto acceptInvitation(UUID invitationId, UUID patientId) {
        PatientInvitation inv = invitationRepo.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is not pending");
        }

        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setPatient(patient);
        invitationRepo.save(inv);

        // Crear acceso
        if (!accessRepo.existsByDoctorIdAndPatientId(inv.getDoctor().getId(), patientId)) {
            DoctorPatientAccess access = new DoctorPatientAccess();
            access.setDoctor(inv.getDoctor());
            access.setPatient(patient);
            accessRepo.save(access);
        }

        logAudit(inv, LinkAuditAction.INVITATION_ACCEPTED, patientId);

        return toDto(inv);
    }

    @Override
    @Transactional
    public InvitationDto rejectInvitation(UUID invitationId, UUID patientId) {
        PatientInvitation inv = invitationRepo.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is not pending");
        }

        inv.setStatus(InvitationStatus.REJECTED);
        invitationRepo.save(inv);

        logAudit(inv, LinkAuditAction.INVITATION_REJECTED, patientId);

        return toDto(inv);
    }

    @Override
    @Transactional
    public void revokeAccess(UUID doctorId, UUID patientId, UUID actorId) {
        DoctorPatientAccess access = accessRepo.findByDoctorIdAndPatientId(doctorId, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Access not found"));

        accessRepo.delete(access);

        // Opcional: revocar la invitación original si la rastreamos, o solo crear un log suelto
        LinkAudit audit = new LinkAudit();
        audit.setAction(LinkAuditAction.ACCESS_REVOKED);
        audit.setActorId(actorId);
        auditRepo.save(audit);
    }

    @Override
    public List<InvitationDto> getPendingInvitationsForDoctor(UUID doctorId) {
        return invitationRepo.findByDoctorId(doctorId).stream()
                .filter(i -> i.getStatus() == InvitationStatus.PENDING)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvitationDto> getInvitationsForPatient(UUID patientId, String email, Integer dni) {
        List<PatientInvitation> list = new ArrayList<>(invitationRepo.findByPatientId(patientId));
        if (email != null) {
            list.addAll(invitationRepo.findByPatientEmail(email));
        }
        if (dni != null) {
            list.addAll(invitationRepo.findByPatientDni(dni));
        }

        return list.stream()
                .distinct()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void logAudit(PatientInvitation inv, LinkAuditAction action, UUID actorId) {
        LinkAudit audit = new LinkAudit();
        audit.setInvitation(inv);
        audit.setAction(action);
        audit.setActorId(actorId);
        auditRepo.save(audit);
    }

    private InvitationDto toDto(PatientInvitation inv) {
        InvitationDto dto = new InvitationDto();
        dto.setId(inv.getId());
        dto.setDoctorId(inv.getDoctor().getId());
        dto.setDoctorName(inv.getDoctor().getName()); // Asumiendo que doctor tiene getName()
        dto.setPatientDni(inv.getPatientDni());
        dto.setPatientEmail(inv.getPatientEmail());
        dto.setStatus(inv.getStatus());
        dto.setCreatedAt(inv.getCreatedAt());
        dto.setExpiresAt(inv.getExpiresAt());
        return dto;
    }
}
