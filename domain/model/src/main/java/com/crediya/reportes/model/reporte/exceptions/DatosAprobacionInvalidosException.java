package com.crediya.reportes.model.reporte.exceptions;

import com.crediya.reportes.model.common.exceptions.DomainException;

public class DatosAprobacionInvalidosException extends DomainException {

    public static final String CODE = "DATOS_APROBACION_INVALIDOS";

    private final String campo;

    public DatosAprobacionInvalidosException(String campo, String motivo) {
        super(CODE, "El campo '" + campo + "' " + motivo);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
