package com.agustin.backend_dialysis_record.invitation;

import com.agustin.backend_dialysis_record.controller.InvitationController;
import com.agustin.backend_dialysis_record.dto.CreateInvitationDto;
import com.agustin.backend_dialysis_record.dto.InvitationDto;
import com.agustin.backend_dialysis_record.repository.UserAccountRepository;
import com.agustin.backend_dialysis_record.service.InvitationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
@AutoConfigureMockMvc
public class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "DOCTOR")
    void testCreateInvitation() throws Exception {
        // Arrange
        UUID authId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID userAccountId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        com.agustin.backend_dialysis_record.model.auth.UserAccount mockUa = new com.agustin.backend_dialysis_record.model.auth.UserAccount();
        mockUa.setId(userAccountId);
        
        when(userAccountRepository.findByAuthId(authId)).thenReturn(Optional.of(mockUa));
        when(userAccountRepository.findDoctorIdByUserAccountId(userAccountId)).thenReturn(Optional.of(doctorId));

        CreateInvitationDto requestDto = new CreateInvitationDto();
        requestDto.setPatientEmail("patient@example.com");

        InvitationDto responseDto = new InvitationDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setPatientEmail("patient@example.com");

        when(invitationService.createInvitation(eq(doctorId), any(CreateInvitationDto.class)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/invitations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientEmail").value("patient@example.com"));
    }
}
