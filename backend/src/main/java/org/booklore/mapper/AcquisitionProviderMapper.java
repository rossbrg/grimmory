package org.booklore.mapper;

import org.booklore.model.dto.AcquisitionProviderConfig;
import org.booklore.model.entity.AcquisitionProviderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = org.booklore.acquisition.AcquisitionContentTypes.class)
public interface AcquisitionProviderMapper {

    @Mapping(target = "allowedContentTypes", expression = "java(AcquisitionContentTypes.toList(entity.getAllowedContentTypes()))")
    @Mapping(target = "tokenSet", expression = "java(entity.getApiToken() != null && !entity.getApiToken().isBlank())")
    AcquisitionProviderConfig toDto(AcquisitionProviderEntity entity);
}
