package com.crediya.reportes.sqs.listener;

import com.crediya.reportes.model.reporte.commands.RegistrarAprobacionCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Contrato del mensaje que publica el servicio de solicitudes cuando una solicitud queda aprobada (version 1). Es un lector
 * tolerante: ignora los campos que no conoce y solo exige los que necesita.
 * <pre>
 * {"version":1,"id_evento":"..","tipo":"SOLICITUD_APROBADA","ocurrido_en":"..",
 *  "solicitud":{"id_solicitud":"..","monto":2000000,"plazo":24,"tipo_prestamo":"PERSONAL"}}
 * </pre>
 */
final class SolicitudAprobadaMensaje {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int VERSION_SOPORTADA = 1;

    private SolicitudAprobadaMensaje() {
    }

    /**
     * @throws IllegalArgumentException si el mensaje no es JSON o no cumple el contrato
     */
    static RegistrarAprobacionCommand aComando(String cuerpo) {
        JsonNode raiz = leer(cuerpo);
        if (raiz.path("version").asInt(VERSION_SOPORTADA) != VERSION_SOPORTADA) {
            throw new IllegalArgumentException("Version del mensaje no soportada: " + raiz.path("version").asText());
        }
        JsonNode solicitud = raiz.path("solicitud");
        JsonNode id = solicitud.path("id_solicitud");
        JsonNode monto = solicitud.path("monto");
        if (!id.isTextual() || id.asText().isBlank()) {
            throw new IllegalArgumentException("El mensaje debe traer solicitud.id_solicitud");
        }
        if (!monto.isNumber()) {
            throw new IllegalArgumentException("El mensaje debe traer solicitud.monto numerico");
        }
        return new RegistrarAprobacionCommand(id.asText(), monto.decimalValue());
    }

    private static JsonNode leer(String cuerpo) {
        try {
            JsonNode raiz = MAPPER.readTree(cuerpo);
            if (raiz == null || !raiz.isObject()) {
                throw new IllegalArgumentException("El mensaje no es un objeto JSON");
            }
            return raiz;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("El mensaje no es JSON valido", e);
        }
    }
}
