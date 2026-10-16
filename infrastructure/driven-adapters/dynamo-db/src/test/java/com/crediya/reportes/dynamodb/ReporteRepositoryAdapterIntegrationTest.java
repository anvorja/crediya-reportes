package com.crediya.reportes.dynamodb;

import com.crediya.reportes.dynamodb.config.DynamoDBProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DisplayName("ReporteRepositoryAdapter (DynamoDB real en LocalStack)")
class ReporteRepositoryAdapterIntegrationTest {

    private static final String TABLA = "crediya-reportes-test";
    private static final Instant AHORA = Instant.parse("2026-10-16T12:00:00Z");

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.9.2"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private static DynamoDbAsyncClient cliente;
    private ReporteRepositoryAdapter repositorio;

    @BeforeAll
    static void iniciar() {
        cliente = DynamoDbAsyncClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();
    }

    @AfterAll
    static void cerrar() {
        cliente.close();
    }

    @BeforeEach
    void tablaLimpia() {
        cliente.deleteTable(r -> r.tableName(TABLA)).exceptionally(e -> null).join();
        cliente.createTable(r -> r.tableName(TABLA)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .keySchema(k -> k.attributeName("pk").keyType(KeyType.HASH))
                .attributeDefinitions(a -> a.attributeName("pk").attributeType(ScalarAttributeType.S))).join();
        repositorio = new ReporteRepositoryAdapter(cliente, new DynamoDBProperties("us-east-1", null, TABLA, 30),
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("sin aprobaciones, obtener no devuelve nada (el caso de uso lo convierte en un reporte en ceros)")
    void sinDatos() {
        StepVerifier.create(repositorio.obtener()).verifyComplete();
    }

    @Test
    @DisplayName("una aprobacion nueva se cuenta y suma su monto")
    void primeraAprobacion() {
        StepVerifier.create(repositorio.registrarAprobacion("sol-1", new BigDecimal("2000000.50"))).expectNext(true).verifyComplete();

        StepVerifier.create(repositorio.obtener())
                .assertNext(r -> {
                    assertThat(r.totalAprobadas()).isEqualTo(1);
                    assertThat(r.montoTotalAprobado()).isEqualByComparingTo("2000000.50");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("el mismo aviso entregado dos veces no suma dos veces")
    void avisoRepetido() {
        repositorio.registrarAprobacion("sol-1", new BigDecimal("1000000")).block();

        StepVerifier.create(repositorio.registrarAprobacion("sol-1", new BigDecimal("1000000"))).expectNext(false).verifyComplete();

        StepVerifier.create(repositorio.obtener())
                .assertNext(r -> {
                    assertThat(r.totalAprobadas()).isEqualTo(1);
                    assertThat(r.montoTotalAprobado()).isEqualByComparingTo("1000000");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("solicitudes distintas se acumulan")
    void variasAprobaciones() {
        repositorio.registrarAprobacion("sol-1", new BigDecimal("1000000")).block();
        repositorio.registrarAprobacion("sol-2", new BigDecimal("2500000.25")).block();

        StepVerifier.create(repositorio.obtener())
                .assertNext(r -> {
                    assertThat(r.totalAprobadas()).isEqualTo(2);
                    assertThat(r.montoTotalAprobado()).isEqualByComparingTo("3500000.25");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("la misma solicitud llegando a la vez desde varios hilos se cuenta exactamente una vez")
    void concurrencia() {
        List<Boolean> resultados = Flux.range(0, 15)
                .flatMap(i -> repositorio.registrarAprobacion("sol-concurrente", new BigDecimal("500000")), 15)
                .collectList().block();

        assertThat(resultados).containsOnlyOnce(true).hasSize(15);
        StepVerifier.create(repositorio.obtener())
                .assertNext(r -> {
                    assertThat(r.totalAprobadas()).isEqualTo(1);
                    assertThat(r.montoTotalAprobado()).isEqualByComparingTo("500000");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("la marca de solicitud contada expira (TTL) a los dias configurados")
    void marcaConTtl() {
        repositorio.registrarAprobacion("sol-ttl", BigDecimal.TEN).block();

        var item = cliente.getItem(r -> r.tableName(TABLA).key(Map.of("pk", AttributeValue.builder().s("APROBACION#sol-ttl").build()))).join().item();

        assertThat(item.get("expira_en").n()).isEqualTo(String.valueOf(AHORA.plusSeconds(30L * 24 * 3600).getEpochSecond()));
        assertThat(item.get("registrado_en").s()).isEqualTo(AHORA.toString());
    }

    @Test
    @DisplayName("un error distinto a 'ya contada' (por ejemplo tabla inexistente) no se oculta")
    void errorInesperado_seRelanza() {
        var repoSinTabla = new ReporteRepositoryAdapter(cliente, new DynamoDBProperties("us-east-1", null, "no-existe", 30),
                Clock.fixed(AHORA, ZoneOffset.UTC));

        StepVerifier.create(Mono.defer(() -> repoSinTabla.registrarAprobacion("sol-x", BigDecimal.ONE))).expectError().verify();
    }
}
