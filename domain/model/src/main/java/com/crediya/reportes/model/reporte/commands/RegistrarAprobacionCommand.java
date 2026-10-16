package com.crediya.reportes.model.reporte.commands;

import java.math.BigDecimal;

public record RegistrarAprobacionCommand(String idSolicitud, BigDecimal monto) {
}
