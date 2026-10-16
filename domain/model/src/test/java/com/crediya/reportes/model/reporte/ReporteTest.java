package com.crediya.reportes.model.reporte;

import com.crediya.reportes.model.reporte.exceptions.DatosAprobacionInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reporte")
class ReporteTest {

    @Test
    @DisplayName("el reporte vacio no tiene aprobadas ni monto")
    void vacio() {
        var reporte = Reporte.vacio();

        assertThat(reporte.totalAprobadas()).isZero();
        assertThat(reporte.montoTotalAprobado()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("un monto nulo se toma como cero")
    void montoNuloEsCero() {
        assertThat(new Reporte(3, null).montoTotalAprobado()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("conserva el total y el monto acumulado")
    void conservaValores() {
        var reporte = new Reporte(2, new BigDecimal("7500000.50"));

        assertThat(reporte.totalAprobadas()).isEqualTo(2);
        assertThat(reporte.montoTotalAprobado()).isEqualByComparingTo("7500000.50");
    }

    @Test
    @DisplayName("la excepcion de datos invalidos explica el campo y lleva un codigo estable")
    void excepcionDatosInvalidos() {
        var error = new DatosAprobacionInvalidosException("monto", "debe ser mayor que cero");

        assertThat(error.getCode()).isEqualTo("DATOS_APROBACION_INVALIDOS");
        assertThat(error.getCampo()).isEqualTo("monto");
        assertThat(error.getMessage()).isEqualTo("El campo 'monto' debe ser mayor que cero");
    }

    @Test
    @DisplayName("la excepcion de acceso denegado tiene codigo y mensaje estables")
    void excepcionAccesoDenegado() {
        var error = new com.crediya.reportes.model.common.exceptions.AccesoDenegadoException();

        assertThat(error.getCode()).isEqualTo("ACCESO_DENEGADO");
        assertThat(error.getMessage()).isEqualTo("No tienes permisos para realizar esta accion");
    }
}
