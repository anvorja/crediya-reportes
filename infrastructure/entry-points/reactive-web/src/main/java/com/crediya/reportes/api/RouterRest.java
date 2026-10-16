package com.crediya.reportes.api;

import com.crediya.reportes.api.constants.ApiRoutes;
import com.crediya.reportes.api.documentation.ReporteOpenApi;
import com.crediya.reportes.api.reporte.ReporteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

@Configuration
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> routerFunction(ReporteHandler reporteHandler) {
        return route()
                .GET(ApiRoutes.REPORTE_SOLICITUDES_APROBADAS, accept(MediaType.APPLICATION_JSON),
                        reporteHandler::solicitudesAprobadas, ReporteOpenApi::solicitudesAprobadas)
                .build();
    }
}
