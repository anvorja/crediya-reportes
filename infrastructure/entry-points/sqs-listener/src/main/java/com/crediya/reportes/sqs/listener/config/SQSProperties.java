package com.crediya.reportes.sqs.listener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * La conexion a AWS (region, endpoint, credenciales) la configura {@code SQSClientConfig}; aqui solo se
 * describe que cola se escucha y como.
 */
@ConfigurationProperties(prefix = "entrypoint.sqs")
public record SQSProperties(
        boolean enabled,
        String queueUrl,
        int waitTimeSeconds,
        int visibilityTimeoutSeconds,
        int maxNumberOfMessages,
        int numberOfThreads) {
}
