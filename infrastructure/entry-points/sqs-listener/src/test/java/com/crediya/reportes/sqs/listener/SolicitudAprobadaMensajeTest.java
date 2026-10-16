package com.crediya.reportes.sqs.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Contrato del mensaje SOLICITUD_APROBADA")
class SolicitudAprobadaMensajeTest {

    @Test
    @DisplayName("lee el id y el monto e ignora los campos que no necesita")
    void mensajeValido() {
        var comando = SolicitudAprobadaMensaje.aComando("""
                {"version":1,"id_evento":"e1","tipo":"SOLICITUD_APROBADA","ocurrido_en":"2026-10-16T12:00:00Z",
                 "solicitud":{"id_solicitud":"abc-1","monto":2000000.50,"plazo":24,"tipo_prestamo":"PERSONAL"},"extra":true}""");

        assertThat(comando.idSolicitud()).isEqualTo("abc-1");
        assertThat(comando.monto()).isEqualByComparingTo("2000000.50");
    }

    @Test
    @DisplayName("sin version se asume la 1")
    void sinVersion() {
        assertThat(SolicitudAprobadaMensaje.aComando("{\"solicitud\":{\"id_solicitud\":\"a\",\"monto\":1}}").idSolicitud()).isEqualTo("a");
    }

    @Test
    @DisplayName("una version distinta de la 1 se rechaza")
    void versionNoSoportada() {
        assertThatThrownBy(() -> SolicitudAprobadaMensaje.aComando("{\"version\":2,\"solicitud\":{\"id_solicitud\":\"a\",\"monto\":1}}"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Version");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"solicitud\":{\"monto\":1}}",
            "{\"solicitud\":{\"id_solicitud\":\"  \",\"monto\":1}}",
            "{\"solicitud\":{\"id_solicitud\":5,\"monto\":1}}",
            "{}"})
    @DisplayName("sin id de solicitud se rechaza")
    void sinId(String cuerpo) {
        assertThatThrownBy(() -> SolicitudAprobadaMensaje.aComando(cuerpo))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("id_solicitud");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"solicitud\":{\"id_solicitud\":\"a\"}}",
            "{\"solicitud\":{\"id_solicitud\":\"a\",\"monto\":\"mucho\"}}"})
    @DisplayName("sin monto numerico se rechaza")
    void sinMonto(String cuerpo) {
        assertThatThrownBy(() -> SolicitudAprobadaMensaje.aComando(cuerpo))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("monto");
    }

    @ParameterizedTest
    @ValueSource(strings = {"esto no es json", "[1,2]", "\"texto\"", ""})
    @DisplayName("lo que no es un objeto JSON se rechaza")
    void noEsJson(String cuerpo) {
        assertThatThrownBy(() -> SolicitudAprobadaMensaje.aComando(cuerpo)).isInstanceOf(IllegalArgumentException.class);
    }
}
