package com.crediya.excepcion;

/**
 * Los datos son validos por separado pero la operacion rompe una regla del
 * negocio: abonar mas de lo que se debe, prestar a un cliente moroso,
 * registrar dos empleados con el mismo documento.
 */
public class ReglaNegocioException extends CrediYaException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
