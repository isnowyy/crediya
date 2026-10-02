package com.crediya.excepcion;

/**
 * Raiz de todas las excepciones propias del sistema.
 *
 * <p>Se extiende de {@link RuntimeException} de forma deliberada: ninguna capa
 * intermedia sabria como recuperarse de estos errores, asi que obligarlas a
 * declarar {@code throws} solo ensuciaria las firmas. El unico punto que las
 * atrapa es el menu de consola, que le muestra el mensaje al usuario y sigue
 * ejecutando.</p>
 */
public abstract class CrediYaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected CrediYaException(String mensaje) {
        super(mensaje);
    }

    protected CrediYaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
