package com.agustin.backend_dialysis_record.service;

import com.agustin.backend_dialysis_record.dto.SessionDto;
import com.agustin.backend_dialysis_record.dto.SessionSummaryDto;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SessionService {
    List<SessionDto> findAll();

    SessionDto findById(UUID id);

    SessionDto create(SessionDto sessionDto);

    SessionDto update(UUID id, SessionDto sessionDto);

    void delete(UUID id);

    List<SessionDto> findSessionsByPatientId(UUID patientId);

    List<SessionDto> findSessionsByPatientIdAndDateRange(UUID patientId, LocalDate startDate, LocalDate endDate);

    List<SessionDto> findSessionsByDay(UUID patientId, LocalDate day);

    SessionDto createForPatient(UUID patientId, @Valid SessionDto sessionDto);

    SessionSummaryDto summarizeSessionsByDay(UUID patientId, LocalDate day);

    SessionSummaryDto summarizeSessionsByMonth(UUID patientId, int year, int month);
}
