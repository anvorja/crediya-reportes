package com.crediya.reportes.usecase.obtenerreporte;

import com.crediya.reportes.model.reporte.Reporte;
import com.crediya.reportes.model.reporte.gateways.ReporteRepository;
import reactor.core.publisher.Mono;

public class ObtenerReporteUseCase {

    private final ReporteRepository reporteRepository;

    public ObtenerReporteUseCase(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    public Mono<Reporte> ejecutar() {
        return reporteRepository.obtener().defaultIfEmpty(Reporte.vacio());
    }
}
