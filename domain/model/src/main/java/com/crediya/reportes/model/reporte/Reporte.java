package com.crediya.reportes.model.reporte;

import java.math.BigDecimal;

/**
 * Totales de las solicitudes aprobadas. Es un acumulado: cada aprobacion se cuenta una sola vez.
 */
public record Reporte(long totalAprobadas, BigDecimal montoTotalAprobado) {

    public Reporte {
        montoTotalAprobado = montoTotalAprobado == null ? BigDecimal.ZERO : montoTotalAprobado;
    }

    /** Reporte de un sistema que todavia no ha aprobado nada. */
    public static Reporte vacio() {
        return new Reporte(0, BigDecimal.ZERO);
    }
}
