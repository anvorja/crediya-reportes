package com.crediya.reportes.integration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio completo de punta a punta: aviso en SQS (LocalStack) -> contador en DynamoDB (LocalStack) -> API protegida con JWT.
 * AUTENTICACION se simula con un servidor HTTP que solo publica la clave publica (JWKS).
 */
@Tag("integration")
@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Reportes - solicitudes aprobadas (aplicacion completa)")
class ReportesIntegrationTest {

    private static final String RUTA = "/api/v1/reportes/solicitudes-aprobadas";
    private static final String COLA = "crediya-solicitud-aprobada";
    private static final String TABLA = "crediya-reportes";

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.9.2"))
            .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.DYNAMODB);
    static final MockWebServer AUTENTICACION = new MockWebServer();
    static final RSAKey CLAVE = generarClave();

    private static SqsAsyncClient sqs;
    private static DynamoDbAsyncClient dynamo;
    private static String urlCola;

    static {
        AUTENTICACION.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest peticion) {
                if ("/.well-known/jwks.json".equals(peticion.getPath())) {
                    return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                            .setBody(new JWKSet(CLAVE.toPublicJWK()).toString());
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        try {
            AUTENTICACION.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    private WebTestClient client;

    private static RSAKey generarClave() {
        try {
            return new RSAKeyGenerator(2048).keyID("k1").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        // El SDK de la aplicacion toma las credenciales de las propiedades del sistema (no hay nada real en el codigo)
        System.setProperty("aws.accessKeyId", LOCALSTACK.getAccessKey());
        System.setProperty("aws.secretAccessKey", LOCALSTACK.getSecretKey());
        var credenciales = StaticCredentialsProvider.create(AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        sqs = SqsAsyncClient.builder().endpointOverride(LOCALSTACK.getEndpoint()).region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(credenciales).build();
        dynamo = DynamoDbAsyncClient.builder().endpointOverride(LOCALSTACK.getEndpoint()).region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(credenciales).build();
        urlCola = sqs.createQueue(r -> r.queueName(COLA)).join().queueUrl();

        registro.add("AWS_REGION", LOCALSTACK::getRegion);
        registro.add("AWS_ENDPOINT_URL", () -> LOCALSTACK.getEndpoint().toString());
        registro.add("DYNAMODB_TABLE", () -> TABLA);
        registro.add("SQS_APROBADAS_URL", () -> urlCola);
        registro.add("AUTH_JWKS_URI", () -> AUTENTICACION.url("/.well-known/jwks.json").toString());
    }

    @AfterAll
    static void cerrar() throws IOException {
        AUTENTICACION.shutdown();
        sqs.close();
        dynamo.close();
    }

    @BeforeEach
    void limpiar() {
        dynamo.deleteTable(r -> r.tableName(TABLA)).exceptionally(e -> null).join();
        dynamo.createTable(r -> r.tableName(TABLA)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .keySchema(k -> k.attributeName("pk").keyType(KeyType.HASH))
                .attributeDefinitions(a -> a.attributeName("pk").attributeType(ScalarAttributeType.S))).join();
        sqs.purgeQueue(r -> r.queueUrl(urlCola)).join();
    }

    private static String token(String rol) {
        try {
            var claims = new JWTClaimsSet.Builder().issuer("crediya-auth").audience("crediya").subject(UUID.randomUUID().toString())
                    .claim("rol", rol)
                    .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 60_000));
            var firmado = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(CLAVE.getKeyID()).build(), claims.build());
            firmado.sign(new RSASSASigner(CLAVE));
            return firmado.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private WebTestClient.ResponseSpec consultar(String token) {
        var peticion = client.get().uri(RUTA).accept(MediaType.APPLICATION_JSON);
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return peticion.exchange();
    }

    private static void aviso(String idSolicitud, String monto) {
        sqs.sendMessage(r -> r.queueUrl(urlCola).messageBody("""
                {"version":1,"id_evento":"%s","tipo":"SOLICITUD_APROBADA","ocurrido_en":"2026-10-16T12:00:00Z",
                 "solicitud":{"id_solicitud":"%s","monto":%s,"plazo":24,"tipo_prestamo":"PERSONAL"}}"""
                .formatted(UUID.randomUUID(), idSolicitud, monto))).join();
    }

    private void esperarTotal(int total) {
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
                consultar(token("ADMIN")).expectStatus().isOk().expectBody().jsonPath("$.total_aprobadas").isEqualTo(total));
    }

    @Test
    @DisplayName("solo un ADMIN consulta el reporte: sin token 401, CLIENTE y ASESOR 403")
    void soloAdmin() {
        consultar(null).expectStatus().isUnauthorized();
        consultar(token("CLIENTE")).expectStatus().isForbidden();
        consultar(token("ASESOR")).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("sin aprobaciones el reporte es de ceros, no un error")
    void enCeros() {
        consultar(token("ADMIN")).expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total_aprobadas").isEqualTo(0)
                .jsonPath("$.monto_total_aprobado").isEqualTo(0);
    }

    @Test
    @DisplayName("cada aviso de aprobacion de la cola suma al total y al monto aprobado")
    void avisosSeAcumulan() {
        aviso(UUID.randomUUID().toString(), "2000000");
        aviso(UUID.randomUUID().toString(), "3500000.50");

        esperarTotal(2);
        consultar(token("ADMIN")).expectStatus().isOk()
                .expectBody().jsonPath("$.monto_total_aprobado").isEqualTo(5500000.50);
    }

    @Test
    @DisplayName("el mismo aviso entregado dos veces se cuenta una sola vez")
    void avisoRepetido() {
        String id = UUID.randomUUID().toString();
        aviso(id, "1000000");
        aviso(id, "1000000");
        aviso(UUID.randomUUID().toString(), "500000");

        esperarTotal(2);
        consultar(token("ADMIN")).expectStatus().isOk()
                .expectBody().jsonPath("$.monto_total_aprobado").isEqualTo(1500000);
    }

    @Test
    @DisplayName("un mensaje que rompe el contrato no se cuenta y no impide procesar los demas")
    void mensajeInvalido() {
        sqs.sendMessage(r -> r.queueUrl(urlCola).messageBody("esto no es json")).join();
        aviso(UUID.randomUUID().toString(), "750000");

        esperarTotal(1);
        consultar(token("ADMIN")).expectStatus().isOk()
                .expectBody().jsonPath("$.monto_total_aprobado").isEqualTo(750000);
    }
}
