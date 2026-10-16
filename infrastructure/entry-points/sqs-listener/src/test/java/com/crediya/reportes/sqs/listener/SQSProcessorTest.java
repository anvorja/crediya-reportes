package com.crediya.reportes.sqs.listener;

import com.crediya.reportes.model.reporte.commands.RegistrarAprobacionCommand;
import com.crediya.reportes.usecase.registraraprobacion.RegistrarAprobacionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SQSProcessor")
class SQSProcessorTest {

    private static final String CUERPO = "{\"version\":1,\"solicitud\":{\"id_solicitud\":\"abc-1\",\"monto\":2000000}}";

    @Mock
    private RegistrarAprobacionUseCase registrarAprobacion;

    @InjectMocks
    private SQSProcessor procesador;

    private static Message mensaje(String cuerpo) {
        return Message.builder().messageId("m-1").body(cuerpo).build();
    }

    @Test
    @DisplayName("un aviso valido se convierte en el comando y completa (el mensaje se borra)")
    void avisoValido() {
        var comando = new RegistrarAprobacionCommand("abc-1", new BigDecimal("2000000"));
        when(registrarAprobacion.ejecutar(comando)).thenReturn(Mono.just(true));

        StepVerifier.create(procesador.apply(mensaje(CUERPO))).verifyComplete();

        verify(registrarAprobacion).ejecutar(comando);
    }

    @Test
    @DisplayName("un aviso repetido completa sin error: no se reintenta ni va a la DLQ")
    void avisoRepetido() {
        when(registrarAprobacion.ejecutar(new RegistrarAprobacionCommand("abc-1", new BigDecimal("2000000")))).thenReturn(Mono.just(false));

        StepVerifier.create(procesador.apply(mensaje(CUERPO))).verifyComplete();
    }

    @Test
    @DisplayName("un mensaje que rompe el contrato falla para que la cola lo reintente y termine en la DLQ")
    void mensajeInvalido() {
        StepVerifier.create(procesador.apply(mensaje("basura"))).expectError(IllegalArgumentException.class).verify();

        verifyNoInteractions(registrarAprobacion);
    }

    @Test
    @DisplayName("si el caso de uso falla, el error llega al listener para reintentar")
    void falloDelCasoDeUso() {
        when(registrarAprobacion.ejecutar(new RegistrarAprobacionCommand("abc-1", new BigDecimal("2000000"))))
                .thenReturn(Mono.error(new IllegalStateException("dynamo caido")));

        StepVerifier.create(procesador.apply(mensaje(CUERPO))).expectError(IllegalStateException.class).verify();
    }
}
