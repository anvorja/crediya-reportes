package com.crediya.reportes.api.exception;

import com.crediya.reportes.model.common.exceptions.AccesoDenegadoException;
import com.crediya.reportes.model.reporte.exceptions.DatosAprobacionInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCatalog")
class ErrorCatalogTest {

    @Test
    @DisplayName("debe mapear cada codigo de dominio conocido a su estado HTTP")
    void debeMapearCodigosConocidos() {
        assertThat(ErrorCatalog.estadoDe(DatosAprobacionInvalidosException.CODE)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCatalog.estadoDe(AccesoDenegadoException.CODE)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("cuando el codigo de dominio no esta catalogado, debe responder 422")
    void cuandoCodigoDesconocido_debeResponder422() {
        assertThat(ErrorCatalog.estadoDe("CODIGO_NUEVO")).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
