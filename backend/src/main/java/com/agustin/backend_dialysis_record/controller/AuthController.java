package com.agustin.backend_dialysis_record.controller;

import com.agustin.backend_dialysis_record.dto.auth.RegisterDoctorRequest;
import com.agustin.backend_dialysis_record.dto.auth.RegisterPatientRequest;
import com.agustin.backend_dialysis_record.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<Void> registerDoctor(
            @Valid @RequestBody RegisterDoctorRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID authId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        
        authService.registerDoctor(req, authId, email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/patient")
    public ResponseEntity<Void> registerPatient(
            @Valid @RequestBody RegisterPatientRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID authId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        
        authService.registerPatient(req, authId, email);
        return ResponseEntity.ok().build();
    }

}
