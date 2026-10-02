package com.crediya.excepcion;

/**
 * Falla al leer o escribir en el almacen de datos.
 *
 * <p>Traduce las excepciones tecnicas de cada tecnologia
 * ({@code SQLException}, {@code IOException}) a un tipo propio del dominio.
 * Asi los servicios no quedan atados a JDBC ni a archivos: ese es el punto del
 * patron Repositorio.</p>
 */
public class PersistenciaException extends CrediYaException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }
}
