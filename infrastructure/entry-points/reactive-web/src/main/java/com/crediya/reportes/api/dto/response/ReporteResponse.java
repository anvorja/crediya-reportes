package com.crediya.reportes.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReporteResponse(
        long totalAprobadas,
        BigDecimal montoTotalAprobado
) {
}
