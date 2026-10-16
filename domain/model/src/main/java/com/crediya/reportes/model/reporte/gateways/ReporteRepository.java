package com.crediya.reportes.model.reporte.gateways;

import com.crediya.reportes.model.reporte.Reporte;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ReporteRepository {

    /**
     * Suma la solicitud al acumulado una sola vez: si ya estaba contada no cambia nada.
     * La entrega de SQS es "al menos una vez", asi que el mismo aviso puede llegar repetido.
     *
     * @return {@code true} si se conto ahora, {@code false} si ya estaba contada
     */
    Mono<Boolean> registrarAprobacion(String idSolicitud, BigDecimal monto);

    /** Acumulado actual, o vacio si todavia no se ha contado ninguna aprobacion. */
    Mono<Reporte> obtener();
}
