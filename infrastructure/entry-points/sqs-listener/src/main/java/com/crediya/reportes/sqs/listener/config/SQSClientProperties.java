package com.crediya.reportes.sqs.listener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param endpoint solo para LocalStack; en AWS va vacio (las credenciales vienen del rol de la tarea)
 */
@ConfigurationProperties(prefix = "adapter.sqs")
public record SQSClientProperties(String region, String endpoint) {
}
