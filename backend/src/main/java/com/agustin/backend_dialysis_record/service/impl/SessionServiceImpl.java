package com.agustin.backend_dialysis_record.service.impl;

import com.agustin.backend_dialysis_record.dto.SessionDto;
import com.agustin.backend_dialysis_record.dto.SessionSummaryDto;
import com.agustin.backend_dialysis_record.mapper.SessionMapper;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.Session;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import com.agustin.backend_dialysis_record.repository.SessionRepository;
import com.agustin.backend_dialysis_record.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SessionServiceImpl implements SessionService {
    private static final Set<Float> FIXED_CONCENTRATIONS = Set.of(1.5f, 2.3f, 3.8f);

    private final SessionRepository sessionRepository;
    private final PatientRepository patientRepository;
    private final SessionMapper sessionMapper;

    @Autowired
    public SessionServiceImpl(SessionRepository sessionRepository, PatientRepository patientRepository, SessionMapper sessionMapper) {
        this.sessionRepository = sessionRepository;
        this.patientRepository = patientRepository;
        this.sessionMapper = sessionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> findAll() {
        return sessionRepository.findAll()
                .stream().map(sessionMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionDto findById(UUID id) {
        return sessionRepository.findById(id)
                .map(sessionMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + id));
    }

    @Override
    public SessionDto create(SessionDto sessionDto) {
        Session session = sessionMapper.toEntity(sessionDto);
        session.computeClinicalDate();
        session = sessionRepository.save(session);
        
        if (session.getPatient() != null) {
            recalculateBagsForClinicalDate(session.getPatient().getId(), session.getClinicalDate());
            session = sessionRepository.findById(session.getId()).orElse(session);
        }
        
        return sessionMapper.toDto(session);
    }

    @Override
    public SessionDto update(UUID id, SessionDto sessionDto) {
        if (sessionDto.getId() != null && !sessionDto.getId().equals(id))
            throw new IllegalArgumentException("Path id and DTO id must match");

        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + id));
        
        LocalDate oldClinicalDate = session.getClinicalDate();
        
        validateConcentrationForPatient(session.getPatient(), sessionDto.getConcentration());
        sessionMapper.updateEntityFromDTO(session, sessionDto);
        session.computeClinicalDate();
        session = sessionRepository.save(session);
        
        if (session.getPatient() != null) {
            UUID patientId = session.getPatient().getId();
            if (oldClinicalDate != null && !oldClinicalDate.equals(session.getClinicalDate())) {
                recalculateBagsForClinicalDate(patientId, oldClinicalDate);
            }
            recalculateBagsForClinicalDate(patientId, session.getClinicalDate());
            session = sessionRepository.findById(session.getId()).orElse(session);
        }
        
        return sessionMapper.toDto(session);
    }

    @Override
    public void delete(UUID id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + id));
        
        UUID patientId = session.getPatient() != null ? session.getPatient().getId() : null;
        LocalDate clinicalDate = session.getClinicalDate();
        
        sessionRepository.deleteById(id);
        sessionRepository.flush(); // ensure it's deleted before querying
        
        if (patientId != null) {
            recalculateBagsForClinicalDate(patientId, clinicalDate);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> findSessionsByPatientId(UUID patientId) { //TODO: CHECK NULLS
        return sessionRepository.findByPatientIdOrderByClinicalDateDescDateDescHourDesc(patientId)
                .stream().map(sessionMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> findSessionsByPatientIdAndDateRange(UUID patientId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        return sessionRepository.findByPatientIdAndClinicalDateBetweenOrderByClinicalDateDescDateDescHourDesc(patientId, startDate, endDate)
                .stream().map(sessionMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> findSessionsByDay(UUID patientId, LocalDate day) { //TODO: CHECK NULLS
        return sessionRepository.findByPatientIdAndClinicalDateOrderByDateDescHourDesc(patientId, day)
                .stream().map(sessionMapper::toDto).toList();
    }

    @Override
    public SessionDto createForPatient(UUID patientId, SessionDto sessionDto) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        Session session = sessionMapper.toEntity(sessionDto);
        validateConcentrationForPatient(patient, sessionDto.getConcentration());

        // 3) forzar ownership: paciente desde path
        session.setPatient(patient);

        if (session.getDate() == null) session.setDate(LocalDate.now());
        session.computeClinicalDate();

        Session saved = sessionRepository.save(session);
        
        recalculateBagsForClinicalDate(patientId, saved.getClinicalDate());
        saved = sessionRepository.findById(saved.getId()).orElse(saved);
        
        return sessionMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionSummaryDto summarizeSessionsByDay(UUID patientId, LocalDate day) {
        return summarize(sessionRepository.findByPatientIdAndClinicalDateOrderByDateDescHourDesc(patientId, day));
    }

    @Override
    @Transactional(readOnly = true)
    public SessionSummaryDto summarizeSessionsByMonth(UUID patientId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return summarize(sessionRepository.findByPatientIdAndClinicalDateBetweenOrderByClinicalDateDescDateDescHourDesc(patientId, start, end));
    }

    private SessionSummaryDto summarize(List<Session> sessions) {
        int totalInfusion = sessions.stream().mapToInt(Session::getInfusion).sum();
        int totalDrainage = sessions.stream().mapToInt(Session::getDrainage).sum();
        int totalBalance = sessions.stream().mapToInt(Session::getPartial).sum();
        return new SessionSummaryDto(sessions.size(), totalInfusion, totalDrainage, totalBalance);
    }

    private void validateConcentrationForPatient(Patient patient, Float concentration) {
        if (patient == null) {
            throw new IllegalArgumentException("patient is required to validate concentration");
        }
        if (concentration == null) {
            throw new IllegalArgumentException("concentration is required");
        }

        boolean fixed = FIXED_CONCENTRATIONS.stream()
                .anyMatch(value -> sameConcentration(value, concentration));
        boolean custom = patient.getCustomConcentrations().stream()
                .anyMatch(value -> sameConcentration(value, concentration));

        if (!fixed && !custom) {
            throw new IllegalArgumentException("Esta concentración no está permitida para este paciente");
        }
    }

    private boolean sameConcentration(float a, float b) {
        return Math.abs(a - b) < 0.0001f;
    }

    private void recalculateBagsForClinicalDate(UUID patientId, LocalDate clinicalDate) {
        if (patientId == null || clinicalDate == null) return;
        List<Session> sessions = sessionRepository.findByPatientIdAndClinicalDateOrderByDateAscHourAsc(patientId, clinicalDate);
        int bagNumber = 1;
        for (Session s : sessions) {
            s.setBag(bagNumber++);
        }
        sessionRepository.saveAll(sessions);
    }
}
