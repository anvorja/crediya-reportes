package com.crediya.reportes.usecase.obtenerreporte;

import com.crediya.reportes.model.reporte.Reporte;
import com.crediya.reportes.model.reporte.gateways.ReporteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObtenerReporteUseCase")
class ObtenerReporteUseCaseTest {

    @Mock
    private ReporteRepository reporteRepository;

    @InjectMocks
    private ObtenerReporteUseCase useCase;

    @Test
    @DisplayName("devuelve el acumulado guardado")
    void conDatos() {
        var guardado = new Reporte(4, new BigDecimal("12000000"));
        when(reporteRepository.obtener()).thenReturn(Mono.just(guardado));

        StepVerifier.create(useCase.ejecutar()).expectNext(guardado).verifyComplete();
    }

    @Test
    @DisplayName("si todavia no hay nada contado devuelve un reporte en ceros, no un error")
    void sinDatos() {
        when(reporteRepository.obtener()).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar()).expectNext(Reporte.vacio()).verifyComplete();
    }
}
