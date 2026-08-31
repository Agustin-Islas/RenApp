package com.agustin.backend_dialysis_record.service;

import com.agustin.backend_dialysis_record.dto.CreateInvitationDto;
import com.agustin.backend_dialysis_record.dto.InvitationDto;

import java.util.List;
import java.util.UUID;

public interface InvitationService {
    InvitationDto createInvitation(UUID doctorId, CreateInvitationDto dto);
    InvitationDto acceptInvitation(UUID invitationId, UUID patientId);
    InvitationDto rejectInvitation(UUID invitationId, UUID patientId);
    void revokeAccess(UUID doctorId, UUID patientId, UUID actorId);
    List<InvitationDto> getPendingInvitationsForDoctor(UUID doctorId);
    List<InvitationDto> getInvitationsForPatient(UUID patientId, String email, Integer dni);
}
