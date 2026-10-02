package com.crediya.excepcion;

/**
 * Se pidio una entidad por su identificador y no existe.
 */
public class RecursoNoEncontradoException extends CrediYaException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /** Atajo para el mensaje mas comun: "Cliente con id 7 no existe." */
    public static RecursoNoEncontradoException de(String entidad, Object id) {
        return new RecursoNoEncontradoException(entidad + " con identificador '" + id + "' no existe.");
    }
}
