package com.crediya.reportes.dynamodb;

import com.crediya.reportes.dynamodb.config.DynamoDBProperties;
import com.crediya.reportes.model.reporte.Reporte;
import com.crediya.reportes.model.reporte.gateways.ReporteRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Guarda el acumulado en un solo item ({@code RESUMEN#APROBADAS}) y, por cada solicitud contada, una marca
 * ({@code APROBACION#<id>}) con TTL. La marca y la suma se escriben en una misma transaccion: si la marca ya existe,
 * la transaccion se cancela y el acumulado no cambia. Asi un aviso repetido no suma dos veces.
 */
@Repository
public class ReporteRepositoryAdapter implements ReporteRepository {

    static final String CLAVE_RESUMEN = "RESUMEN#APROBADAS";
    static final String PREFIJO_MARCA = "APROBACION#";
    private static final String PK = "pk";
    private static final String TOTAL = "total_aprobadas";
    private static final String MONTO = "monto_total_aprobado";
    private static final String CONDICION_CODIGO = "ConditionalCheckFailed";

    private final DynamoDbAsyncClient cliente;
    private final String tabla;
    private final Duration recordarPor;
    private final Clock reloj;

    public ReporteRepositoryAdapter(DynamoDbAsyncClient cliente, DynamoDBProperties propiedades, Clock reloj) {
        this.cliente = cliente;
        this.tabla = propiedades.tableName();
        this.recordarPor = Duration.ofDays(propiedades.ttlDias());
        this.reloj = reloj;
    }

    @Override
    public Mono<Boolean> registrarAprobacion(String idSolicitud, BigDecimal monto) {
        var ahora = reloj.instant();
        var marca = TransactWriteItem.builder().put(Put.builder()
                .tableName(tabla)
                .item(Map.of(
                        PK, texto(PREFIJO_MARCA + idSolicitud),
                        "expira_en", numero(String.valueOf(ahora.plus(recordarPor).getEpochSecond())),
                        "registrado_en", texto(ahora.toString())))
                .conditionExpression("attribute_not_exists(" + PK + ")")
                .build()).build();
        var suma = TransactWriteItem.builder().update(Update.builder()
                .tableName(tabla)
                .key(Map.of(PK, texto(CLAVE_RESUMEN)))
                .updateExpression("ADD " + TOTAL + " :uno, " + MONTO + " :monto SET actualizado_en = :ahora")
                .expressionAttributeValues(Map.of(
                        ":uno", numero("1"),
                        ":monto", numero(monto.toPlainString()),
                        ":ahora", texto(ahora.toString())))
                .build()).build();

        return Mono.fromFuture(() -> cliente.transactWriteItems(TransactWriteItemsRequest.builder().transactItems(marca, suma).build()))
                .thenReturn(true)
                .onErrorResume(ReporteRepositoryAdapter::yaContada, error -> Mono.just(false));
    }

    @Override
    public Mono<Reporte> obtener() {
        return Mono.fromFuture(() -> cliente.getItem(GetItemRequest.builder()
                        .tableName(tabla)
                        .key(Map.of(PK, texto(CLAVE_RESUMEN)))
                        .consistentRead(true)
                        .build()))
                .filter(respuesta -> respuesta.hasItem() && !respuesta.item().isEmpty())
                .map(respuesta -> new Reporte(
                        decimal(respuesta.item().get(TOTAL)).longValue(),
                        decimal(respuesta.item().get(MONTO))));
    }

    /** La marca ya existia: el primer item de la transaccion fallo por su condicion. */
    private static boolean yaContada(Throwable error) {
        return error instanceof TransactionCanceledException cancelada
                && cancelada.hasCancellationReasons()
                && !cancelada.cancellationReasons().isEmpty()
                && CONDICION_CODIGO.equals(cancelada.cancellationReasons().get(0).code());
    }

    private static BigDecimal decimal(AttributeValue valor) {
        return valor == null || valor.n() == null ? BigDecimal.ZERO : new BigDecimal(valor.n());
    }

    private static AttributeValue texto(String valor) {
        return AttributeValue.builder().s(valor).build();
    }

    private static AttributeValue numero(String valor) {
        return AttributeValue.builder().n(valor).build();
    }
}
