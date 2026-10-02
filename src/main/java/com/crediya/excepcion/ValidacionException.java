package com.crediya.excepcion;

/**
 * Un dato de entrada no cumple las reglas de formato del dominio:
 * un nombre vacio, un correo sin arroba, un salario negativo.
 */
public class ValidacionException extends CrediYaException {

    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
