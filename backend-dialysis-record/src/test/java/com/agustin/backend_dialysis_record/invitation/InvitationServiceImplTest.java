package com.agustin.backend_dialysis_record.invitation;

import com.agustin.backend_dialysis_record.dto.CreateInvitationDto;
import com.agustin.backend_dialysis_record.dto.InvitationDto;
import com.agustin.backend_dialysis_record.model.Doctor;
import com.agustin.backend_dialysis_record.model.InvitationStatus;
import com.agustin.backend_dialysis_record.model.PatientInvitation;
import com.agustin.backend_dialysis_record.repository.DoctorPatientAccessRepository;
import com.agustin.backend_dialysis_record.repository.DoctorRepository;
import com.agustin.backend_dialysis_record.repository.LinkAuditRepository;
import com.agustin.backend_dialysis_record.repository.PatientInvitationRepository;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.service.impl.InvitationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvitationServiceImplTest {

    @Mock
    private PatientInvitationRepository invitationRepo;
    @Mock
    private DoctorPatientAccessRepository accessRepo;
    @Mock
    private LinkAuditRepository auditRepo;
    @Mock
    private DoctorRepository doctorRepo;
    @Mock
    private PatientRepository patientRepo;
    @Mock
    private UserAccountRepository userAccountRepo;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    private UUID doctorId;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setName("Dr. House");
    }

    @Test
    void testCreateInvitation_Success() {
        // Arrange
        CreateInvitationDto dto = new CreateInvitationDto();
        dto.setPatientEmail("patient@example.com");
        dto.setPatientDni(12345678);

        when(doctorRepo.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(userAccountRepo.findByNormalizedEmail("patient@example.com")).thenReturn(Optional.empty());

        // Act
        InvitationDto result = invitationService.createInvitation(doctorId, dto);

        // Assert
        assertNotNull(result);
        assertEquals("patient@example.com", result.getPatientEmail());
        
        ArgumentCaptor<PatientInvitation> invCaptor = ArgumentCaptor.forClass(PatientInvitation.class);
        verify(invitationRepo, times(1)).save(invCaptor.capture());
        PatientInvitation savedInv = invCaptor.getValue();
        assertEquals(doctor, savedInv.getDoctor());
        assertEquals(InvitationStatus.PENDING, savedInv.getStatus());
        
        verify(auditRepo, times(1)).save(any());
    }
}
