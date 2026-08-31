package com.agustin.backend_dialysis_record.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter @Getter
public class PatientDto {

    private UUID id;

    @NotBlank
    @Size(max = 60)
    private String name;

    @NotBlank
    @Size(max = 60)
    private String surname;

    @NotNull
    @Min(1_000_000)
    @Max(99_999_999)
    private Integer dni;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    @Size(max = 120)
    private String address;

    @NotNull
    @Min(1)
    private Long number;

    private List<Float> customConcentrations = new ArrayList<>();
}
