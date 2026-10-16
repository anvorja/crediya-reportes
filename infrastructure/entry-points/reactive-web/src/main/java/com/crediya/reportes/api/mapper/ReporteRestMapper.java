package com.crediya.reportes.api.mapper;

import com.crediya.reportes.api.dto.response.ReporteResponse;
import com.crediya.reportes.model.reporte.Reporte;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReporteRestMapper {

    ReporteResponse toResponse(Reporte reporte);
}
