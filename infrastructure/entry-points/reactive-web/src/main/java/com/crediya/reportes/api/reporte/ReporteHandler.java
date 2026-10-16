package com.crediya.reportes.api.reporte;

import com.crediya.reportes.api.mapper.ReporteRestMapper;
import com.crediya.reportes.usecase.obtenerreporte.ObtenerReporteUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ReporteHandler {

    private final ObtenerReporteUseCase obtenerReporteUseCase;
    private final ReporteRestMapper mapper;

    /**
     * GET /api/v1/reportes/solicitudes-aprobadas: total de solicitudes aprobadas y su monto acumulado.
     */
    public Mono<ServerResponse> solicitudesAprobadas(ServerRequest request) {
        return obtenerReporteUseCase.ejecutar()
                .map(mapper::toResponse)
                .flatMap(reporte -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(reporte));
    }
}
