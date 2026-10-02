package com.crediya.app.menu;

/**
 * Una entrada de menu: el texto que ve el usuario y lo que pasa al elegirla.
 *
 * <p>Guardar la accion como {@link Runnable} permite declarar los menus como
 * una lista de lambdas en vez de como una cadena de {@code switch}. El menu
 * pasa a ser datos y no codigo de control: agregar una opcion es agregar un
 * elemento a una lista.</p>
 *
 * @param etiqueta texto visible
 * @param accion   codigo que se ejecuta al elegirla
 * @param pausar   si al terminar hay que esperar a que el usuario lea el
 *                 resultado. Es {@code true} para una operacion, que deja algo
 *                 en pantalla, y {@code false} para una opcion que solo abre
 *                 otro menu: ahi no hay nada que leer y el "pulse Enter"
 *                 estorbaria al volver.
 */
public record OpcionMenu(String etiqueta, Runnable accion, boolean pausar) {

    /** Opcion que ejecuta una operacion y deja un resultado en pantalla. */
    public static OpcionMenu de(String etiqueta, Runnable accion) {
        return new OpcionMenu(etiqueta, accion, true);
    }

    /** Opcion que solo da paso a otro menu. */
    public static OpcionMenu submenu(String etiqueta, Menu destino) {
        return new OpcionMenu(etiqueta, destino::ejecutar, false);
    }
}
