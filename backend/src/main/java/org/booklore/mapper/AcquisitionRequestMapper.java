package org.booklore.mapper;

import org.booklore.model.dto.AcquisitionRequestDto;
import org.booklore.model.entity.AcquisitionRequestEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AcquisitionRequestMapper {

    AcquisitionRequestDto toDto(AcquisitionRequestEntity entity);
}
