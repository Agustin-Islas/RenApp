package com.agustin.backend_dialysis_record.mapper;

import com.agustin.backend_dialysis_record.dto.SessionDto;
import com.agustin.backend_dialysis_record.model.Patient;
import com.agustin.backend_dialysis_record.model.Session;
import com.agustin.backend_dialysis_record.repository.PatientRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper implements GenericMapper<Session, SessionDto> {
    private final PatientRepository patientRepository;

    public SessionMapper(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Session toEntity(SessionDto sessionDto) {
        if (sessionDto == null) { return null; }
        
        Session session = new Session();
        session.setDate(sessionDto.getDate());
        session.setHour(sessionDto.getHour());
        session.setBag(sessionDto.getBag());
        session.setConcentration(sessionDto.getConcentration());
        session.setInfusion(sessionDto.getInfusion());
        session.setDrainage(sessionDto.getDrainage());
        session.setObservations(sessionDto.getObservations());
        Patient patient;
        if (sessionDto.getPatientId() != null) {
            patient = patientRepository.getReferenceById(sessionDto.getPatientId());
            patient.addSession(session);
            session.setPatient(patient);
        }
        return session;
    }

    @Override
    public SessionDto toDto(Session session) {
        if (session == null) { return null; }

        SessionDto sessionDto = new SessionDto();
        sessionDto.setId(session.getId());
        sessionDto.setDate(session.getDate());
        sessionDto.setHour(session.getHour());
        sessionDto.setClinicalDate(session.getClinicalDate());
        sessionDto.setBag(session.getBag());
        sessionDto.setConcentration(session.getConcentration());
        sessionDto.setInfusion(session.getInfusion());
        sessionDto.setDrainage(session.getDrainage());
        sessionDto.setPartial(session.getPartial());
        sessionDto.setObservations(session.getObservations());
        sessionDto.setSeverityLevel(session.getSeverityLevel());
        if (session.getPatient() != null) {
            sessionDto.setPatientName(session.getPatient().getName());
            sessionDto.setPatientId(session.getPatient().getId());
        }
        return sessionDto;
    }

    @Override
    public void updateEntityFromDTO(Session session, SessionDto sessionDto) {
        if (session == null || sessionDto == null) { return; }

        session.setDate(sessionDto.getDate());
        session.setHour(sessionDto.getHour());
        session.setBag(sessionDto.getBag());
        session.setConcentration(sessionDto.getConcentration());
        session.setInfusion(sessionDto.getInfusion());
        session.setDrainage(sessionDto.getDrainage());
        session.setObservations(sessionDto.getObservations());
    }
}
