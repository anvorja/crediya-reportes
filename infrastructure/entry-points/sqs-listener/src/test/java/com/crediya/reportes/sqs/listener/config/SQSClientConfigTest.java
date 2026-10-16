package com.crediya.reportes.sqs.listener.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SQSClientConfig")
class SQSClientConfigTest {

    @Test
    @DisplayName("con endpoint (LocalStack) crea el cliente apuntando a el")
    void conEndpoint() {
        try (var cliente = new SQSClientConfig().sqsAsyncClient(new SQSClientProperties("us-east-1", "http://localhost:4566"))) {
            assertThat(cliente.serviceClientConfiguration().endpointOverride()).get().hasToString("http://localhost:4566");
            assertThat(cliente.serviceClientConfiguration().region().id()).isEqualTo("us-east-1");
        }
    }

    @Test
    @DisplayName("sin endpoint (AWS real) usa el endpoint regional por defecto")
    void sinEndpoint() {
        try (var cliente = new SQSClientConfig().sqsAsyncClient(new SQSClientProperties("us-east-1", ""))) {
            assertThat(cliente.serviceClientConfiguration().endpointOverride()).isEmpty();
        }
    }
}
