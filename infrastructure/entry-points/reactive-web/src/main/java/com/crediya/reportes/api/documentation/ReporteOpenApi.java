package com.crediya.reportes.api.documentation;

import com.crediya.reportes.api.config.OpenApiConfig;
import com.crediya.reportes.api.dto.response.ErrorResponse;
import com.crediya.reportes.api.dto.response.ReporteResponse;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.core.fn.builders.securityrequirement.Builder.securityRequirementBuilder;

/**
 * Documentacion OpenAPI separada de la logica: se pasa como parametro al router.
 */
public final class ReporteOpenApi {

    private static final String TAG = "Reportes";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String OK = String.valueOf(HttpStatus.OK.value());
    private static final String UNAUTHORIZED = String.valueOf(HttpStatus.UNAUTHORIZED.value());
    private static final String FORBIDDEN = String.valueOf(HttpStatus.FORBIDDEN.value());
    private static final String SERVER_ERROR = String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());

    private ReporteOpenApi() {
    }

    public static Builder solicitudesAprobadas(Builder builder) {
        return protegida(builder, "El rol no es ADMIN")
                .operationId("reporteSolicitudesAprobadas")
                .summary("Reporte de solicitudes aprobadas")
                .description("Cantidad de solicitudes de prestamo aprobadas y monto total aprobado. El acumulado se actualiza "
                        + "con cada aprobacion que publica el servicio de solicitudes. Solo un ADMIN puede consultarlo.")
                .tag(TAG)
                .response(responseBuilder().responseCode(OK).description("Acumulado actual (en ceros si aun no hay aprobaciones)")
                        .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ReporteResponse.class))))
                .response(error(SERVER_ERROR, "Error interno"));
    }

    /**
     * Esquema Bearer mas los 401 y 403 que puede devolver cualquier endpoint protegido.
     */
    private static Builder protegida(Builder builder, String descripcionForbidden) {
        return builder
                .security(securityRequirementBuilder().name(OpenApiConfig.ESQUEMA_BEARER))
                .response(error(UNAUTHORIZED, "Falta el token o no es valido"))
                .response(error(FORBIDDEN, descripcionForbidden));
    }

    /**
     * Un unico lugar arma las respuestas de error, siempre con el cuerpo real {@link ErrorResponse}.
     */
    private static org.springdoc.core.fn.builders.apiresponse.Builder error(String codigo, String descripcion) {
        return responseBuilder().responseCode(codigo).description(descripcion)
                .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ErrorResponse.class)));
    }
}
