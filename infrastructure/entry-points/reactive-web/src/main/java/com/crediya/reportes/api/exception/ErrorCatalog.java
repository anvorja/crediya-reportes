package com.crediya.reportes.api.exception;

import org.springframework.http.HttpStatus;

import com.crediya.reportes.model.common.exceptions.AccesoDenegadoException;
import com.crediya.reportes.model.reporte.exceptions.DatosAprobacionInvalidosException;

import java.util.Map;

/**
 * Traduce los codigos de error del dominio a estados HTTP. Es el unico lugar que conoce ambos mundos.
 */
public final class ErrorCatalog {

    public static final String CODIGO_VALIDACION = "VALIDACION_FALLIDA";
    public static final String CODIGO_PETICION_INVALIDA = "PETICION_INVALIDA";
    public static final String CODIGO_NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String MENSAJE_NO_AUTENTICADO = "Debes iniciar sesion con un token valido";
    public static final String CODIGO_ERROR_INTERNO = "ERROR_INTERNO";
    public static final String MENSAJE_ERROR_INTERNO = "Ocurrio un error interno. Intenta de nuevo mas tarde.";

    private static final Map<String, HttpStatus> ESTADOS_DOMINIO = Map.of(
            DatosAprobacionInvalidosException.CODE, HttpStatus.BAD_REQUEST,
            AccesoDenegadoException.CODE, HttpStatus.FORBIDDEN
    );

    private ErrorCatalog() {
    }

    public static HttpStatus estadoDe(String codigoDominio) {
        return ESTADOS_DOMINIO.getOrDefault(codigoDominio, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
