package com.crediya.util;

/**
 * Se agoto la entrada estandar: el usuario pulso Ctrl+D o el programa se esta
 * alimentando desde un archivo que ya termino.
 *
 * <p>No hereda de {@code CrediYaException} a proposito. Los menus atrapan los
 * errores de negocio para seguir funcionando, pero esto no es un error: es la
 * senal de que la sesion acabo. Al dejarla fuera de esa jerarquia sube intacta
 * hasta {@code Main}, que cierra el programa con un mensaje, en lugar de
 * quedarse girando en un menu que ya nadie puede responder.</p>
 */
public class EntradaCerradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntradaCerradaException(String mensaje) {
        super(mensaje);
    }
}
