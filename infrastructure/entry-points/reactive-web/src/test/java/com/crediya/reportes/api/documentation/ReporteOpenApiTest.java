package com.crediya.reportes.api.documentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springdoc.core.fn.builders.operation.Builder.operationBuilder;

@DisplayName("ReporteOpenApi")
class ReporteOpenApiTest {

    @Test
    @DisplayName("solicitudesAprobadas debe documentar el esquema Bearer y cada estado que puede devolver")
    void solicitudesAprobadas_debeDocumentarEstados() {
        Operation operacion = ReporteOpenApi.solicitudesAprobadas(operationBuilder()).build().getOperation();

        assertThat(operacion.operationId()).isEqualTo("reporteSolicitudesAprobadas");
        assertThat(Arrays.stream(operacion.security()).map(SecurityRequirement::name).toList()).containsExactly("bearerAuth");
        assertThat(Arrays.stream(operacion.responses()).map(ApiResponse::responseCode).toList())
                .containsExactlyInAnyOrder("200", "401", "403", "500");
    }

    @Test
    @DisplayName("todo error documentado debe usar el esquema ErrorResponse")
    void errores_debenUsarElEsquemaDeError() {
        Operation operacion = ReporteOpenApi.solicitudesAprobadas(operationBuilder()).build().getOperation();
        List<ApiResponse> errores = Arrays.stream(operacion.responses())
                .filter(respuesta -> Integer.parseInt(respuesta.responseCode()) >= 400)
                .toList();

        assertThat(errores).isNotEmpty()
                .allSatisfy(respuesta -> assertThat(respuesta.content()[0].schema().implementation().getSimpleName())
                        .isEqualTo("ErrorResponse"));
    }
}
