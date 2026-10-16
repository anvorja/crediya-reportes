package com.crediya.reportes.api.reporte;

import com.crediya.reportes.api.RouterRest;
import com.crediya.reportes.api.mapper.ReporteRestMapperImpl;
import com.crediya.reportes.model.reporte.Reporte;
import com.crediya.reportes.usecase.obtenerreporte.ObtenerReporteUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GET /api/v1/reportes/solicitudes-aprobadas")
class ReporteRouterTest {

    private static final String RUTA = "/api/v1/reportes/solicitudes-aprobadas";

    private final ObtenerReporteUseCase useCase = mock(ObtenerReporteUseCase.class);
    private final WebTestClient cliente = WebTestClient
            .bindToRouterFunction(new RouterRest().routerFunction(new ReporteHandler(useCase, new ReporteRestMapperImpl())))
            .build();

    @Test
    @DisplayName("responde 200 con el total y el monto en snake_case")
    void devuelveElReporte() {
        when(useCase.ejecutar()).thenReturn(Mono.just(new Reporte(3, new BigDecimal("7500000.50"))));

        cliente.get().uri(RUTA).accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.total_aprobadas").isEqualTo(3)
                .jsonPath("$.monto_total_aprobado").isEqualTo(7500000.50);
    }

    @Test
    @DisplayName("sin aprobaciones responde 200 con ceros, no 404")
    void enCeros() {
        when(useCase.ejecutar()).thenReturn(Mono.just(Reporte.vacio()));

        cliente.get().uri(RUTA).accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total_aprobadas").isEqualTo(0)
                .jsonPath("$.monto_total_aprobado").isEqualTo(0);
    }
}
