package com.crediya.reportes.dynamodb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param endpoint solo para LocalStack; en AWS va vacio (las credenciales vienen del rol de la tarea)
 * @param ttlDias  cuanto tiempo se recuerda una solicitud ya contada, para ignorar avisos repetidos
 */
@ConfigurationProperties(prefix = "adapter.dynamodb")
public record DynamoDBProperties(String region, String endpoint, String tableName, int ttlDias) {
}
