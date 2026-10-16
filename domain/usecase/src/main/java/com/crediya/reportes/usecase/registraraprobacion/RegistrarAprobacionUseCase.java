package com.crediya.reportes.usecase.registraraprobacion;

import com.crediya.reportes.model.reporte.commands.RegistrarAprobacionCommand;
import com.crediya.reportes.model.reporte.exceptions.DatosAprobacionInvalidosException;
import com.crediya.reportes.model.reporte.gateways.ReporteRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public class RegistrarAprobacionUseCase {

    private final ReporteRepository reporteRepository;

    public RegistrarAprobacionUseCase(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    /**
     * @return {@code true} si la aprobacion se conto, {@code false} si ya estaba contada (aviso repetido)
     */
    public Mono<Boolean> ejecutar(RegistrarAprobacionCommand comando) {
        return Mono.fromCallable(() -> validar(comando))
                .flatMap(valido -> reporteRepository.registrarAprobacion(valido.idSolicitud().trim(), valido.monto()));
    }

    private static RegistrarAprobacionCommand validar(RegistrarAprobacionCommand comando) {
        if (comando.idSolicitud() == null || comando.idSolicitud().isBlank()) {
            throw new DatosAprobacionInvalidosException("id_solicitud", "es obligatorio");
        }
        if (comando.monto() == null || comando.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosAprobacionInvalidosException("monto", "debe ser mayor que cero");
        }
        return comando;
    }
}
