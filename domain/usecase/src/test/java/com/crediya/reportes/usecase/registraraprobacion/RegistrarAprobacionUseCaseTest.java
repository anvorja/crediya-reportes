package com.crediya.reportes.usecase.registraraprobacion;

import com.crediya.reportes.model.reporte.commands.RegistrarAprobacionCommand;
import com.crediya.reportes.model.reporte.exceptions.DatosAprobacionInvalidosException;
import com.crediya.reportes.model.reporte.gateways.ReporteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarAprobacionUseCase")
class RegistrarAprobacionUseCaseTest {

    @Mock
    private ReporteRepository reporteRepository;

    @InjectMocks
    private RegistrarAprobacionUseCase useCase;

    @Test
    @DisplayName("una aprobacion nueva se cuenta y se pasa el id sin espacios")
    void aprobacionNueva() {
        when(reporteRepository.registrarAprobacion("abc-1", new BigDecimal("2000000"))).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(new RegistrarAprobacionCommand("  abc-1 ", new BigDecimal("2000000"))))
                .expectNext(true)
                .verifyComplete();

        verify(reporteRepository).registrarAprobacion("abc-1", new BigDecimal("2000000"));
    }

    @Test
    @DisplayName("un aviso repetido no se cuenta otra vez y no es un error")
    void avisoRepetido() {
        when(reporteRepository.registrarAprobacion(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.ejecutar(new RegistrarAprobacionCommand("abc-1", BigDecimal.TEN)))
                .expectNext(false)
                .verifyComplete();
    }

    @ParameterizedTest(name = "id = [{0}]")
    @ValueSource(strings = {"", "   "})
    @DisplayName("sin id de solicitud falla antes de tocar el repositorio")
    void idInvalido(String id) {
        StepVerifier.create(useCase.ejecutar(new RegistrarAprobacionCommand(id, BigDecimal.TEN)))
                .expectErrorSatisfies(e -> org.assertj.core.api.Assertions.assertThat(e)
                        .isInstanceOf(DatosAprobacionInvalidosException.class)
                        .hasMessageContaining("id_solicitud"))
                .verify();

        verifyNoInteractions(reporteRepository);
    }

    @Test
    @DisplayName("un id nulo tambien falla")
    void idNulo() {
        StepVerifier.create(useCase.ejecutar(new RegistrarAprobacionCommand(null, BigDecimal.TEN)))
                .expectError(DatosAprobacionInvalidosException.class)
                .verify();

        verifyNoInteractions(reporteRepository);
    }

    @ParameterizedTest(name = "monto = {0}")
    @ValueSource(strings = {"0", "-1", "-0.01"})
    @DisplayName("un monto cero o negativo falla antes de tocar el repositorio")
    void montoInvalido(String monto) {
        StepVerifier.create(useCase.ejecutar(new RegistrarAprobacionCommand("abc-1", new BigDecimal(monto))))
                .expectErrorSatisfies(e -> org.assertj.core.api.Assertions.assertThat(e)
                        .isInstanceOf(DatosAprobacionInvalidosException.class)
                        .hasMessageContaining("monto"))
                .verify();

        verifyNoInteractions(reporteRepository);
    }

    @Test
    @DisplayName("un monto nulo tambien falla")
    void montoNulo() {
        StepVerifier.create(useCase.ejecutar(new RegistrarAprobacionCommand("abc-1", null)))
                .expectError(DatosAprobacionInvalidosException.class)
                .verify();

        verifyNoInteractions(reporteRepository);
    }
}
