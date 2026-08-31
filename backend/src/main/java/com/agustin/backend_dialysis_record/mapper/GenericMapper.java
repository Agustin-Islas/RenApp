package com.agustin.backend_dialysis_record.mapper;

import com.agustin.backend_dialysis_record.dto.DoctorDto;
import com.agustin.backend_dialysis_record.model.Doctor;

public interface GenericMapper<E, D> {
    E toEntity(D dto);

    D toDto(E entity);

    void updateEntityFromDTO(E entity, D dto);
}
