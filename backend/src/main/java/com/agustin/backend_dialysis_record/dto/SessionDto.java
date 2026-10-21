package com.agustin.backend_dialysis_record.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Setter @Getter
public class SessionDto {
    private UUID id;

    @NotNull(message = "date is required")
    @PastOrPresent(message = "date cannot be in the future")
    private LocalDate date;

    @NotNull(message = "hour is required")
    private LocalTime hour;

    private LocalDate clinicalDate;

    @NotNull(message = "bag is required")
    @Min(value = 0, message = "bag must be >= 0 ")
    private Integer bag;

    // libre, pero no negativa
    @NotNull(message = "concentration is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "concentration must be >= 0")
    private Float concentration;

    // ml, rango tipico 1000-2500, maximo estricto 5000
    @NotNull(message = "infusion is required")
    @Min(value = 0, message = "infusion must be >= 0 (ml)")
    @Max(value = 5000, message = "infusion must be <= 5000 (ml)")
    private Integer infusion;

    // ml, rango tipico 1500-3200, maximo holgado 5000
    @NotNull(message = "drainage is required")
    @Min(value = 0, message = "drainage must be >= 0 (ml)")
    @Max(value = 5000, message = "drainage must be <= 5000 (ml)")
    private Integer drainage;

    private Integer partial;

    @Size(max = 500, message = "observations max length is 500")
    private String observations;
    
    private Integer severityLevel;

    // se completa solo
    @Size(max = 120, message = "patientName max length is 120")
    private String patientName;

    // se completa solo
    private UUID patientId;
}
