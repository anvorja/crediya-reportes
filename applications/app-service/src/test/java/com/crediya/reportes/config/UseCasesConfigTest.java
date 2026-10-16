package com.crediya.reportes.config;

import com.crediya.reportes.model.reporte.gateways.ReporteRepository;
import com.crediya.reportes.usecase.obtenerreporte.ObtenerReporteUseCase;
import com.crediya.reportes.usecase.registraraprobacion.RegistrarAprobacionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UseCasesConfig: registro automatico de casos de uso")
class UseCasesConfigTest {

    @Test
    @DisplayName("debe registrar como bean cada clase *UseCase con sus puertos inyectados")
    void debeRegistrarCasosDeUso() {
        try (var contexto = new AnnotationConfigApplicationContext(PuertosSimulados.class)) {
            assertThat(contexto.getBeansOfType(RegistrarAprobacionUseCase.class)).hasSize(1);
            assertThat(contexto.getBeansOfType(ObtenerReporteUseCase.class)).hasSize(1);
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class PuertosSimulados {

        @Bean
        ReporteRepository reporteRepository() {
            return Mockito.mock(ReporteRepository.class);
        }
    }
}
