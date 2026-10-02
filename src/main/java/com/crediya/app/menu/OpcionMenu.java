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
 */
public record OpcionMenu(String etiqueta, Runnable accion) {

    public static OpcionMenu de(String etiqueta, Runnable accion) {
        return new OpcionMenu(etiqueta, accion);
    }
}
