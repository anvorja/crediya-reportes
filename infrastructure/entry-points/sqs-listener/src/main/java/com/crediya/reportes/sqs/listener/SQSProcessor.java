package com.crediya.reportes.sqs.listener;

import com.crediya.reportes.usecase.registraraprobacion.RegistrarAprobacionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.function.Function;

/**
 * Procesa los avisos de solicitudes aprobadas. Un mensaje que no cumple el contrato falla y, tras los reintentos de la cola,
 * termina en su DLQ. Un aviso repetido no es un error: se ignora y el mensaje se borra.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {

    private final RegistrarAprobacionUseCase registrarAprobacion;

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromCallable(() -> SolicitudAprobadaMensaje.aComando(message.body()))
                .flatMap(comando -> registrarAprobacion.ejecutar(comando)
                        .doOnNext(contada -> log.info(contada
                                ? "Aprobacion contada solicitud={}"
                                : "Aprobacion ya contada, se ignora solicitud={}", comando.idSolicitud())))
                .then();
    }
}
