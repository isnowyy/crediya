package com.crediya.app.menu;

import com.crediya.excepcion.CrediYaException;
import com.crediya.util.Bitacora;
import com.crediya.util.Consola;
import com.crediya.util.EntradaCerradaException;

import java.util.List;

/**
 * Comportamiento comun de todos los menus (patron Template Method).
 *
 * <p>El ciclo mostrar-leer-ejecutar-repetir se escribe una sola vez aqui. Cada
 * menu concreto solo declara su titulo y su lista de opciones.</p>
 *
 * <p>Aqui esta tambien el unico punto del programa donde se atrapan los errores
 * de negocio. La decision de centralizarlos es deliberada: las clases de
 * dominio y los servicios lanzan excepciones con un mensaje claro y se olvidan
 * del asunto; el menu las traduce en una linea para el usuario, deja la traza
 * completa en la bitacora y vuelve a mostrar el menu. Asi un dato malo nunca
 * tumba el programa ni obliga a ensuciar la logica con try-catch.</p>
 */
public abstract class Menu {

    protected final Consola consola;

    protected Menu(Consola consola) {
        this.consola = consola;
    }

    protected abstract String titulo();

    protected abstract List<OpcionMenu> opciones();

    /** Texto de la opcion cero. El menu principal lo cambia por "Salir". */
    protected String etiquetaSalida() {
        return "Volver al menu anterior";
    }

    /** Se ejecuta antes de dibujar el menu. Util para mostrar un encabezado. */
    protected void alEntrar() {
        // Por defecto no hace nada.
    }

    public void ejecutar() {
        alEntrar();

        while (true) {
            List<OpcionMenu> disponibles = opciones();

            consola.titulo(titulo());
            for (int i = 0; i < disponibles.size(); i++) {
                consola.mostrar("  %d. %s", i + 1, disponibles.get(i).etiqueta());
            }
            consola.mostrar("  0. %s", etiquetaSalida());
            consola.saltoDeLinea();

            int eleccion = consola.leerEntero("  Opcion: ", 0, disponibles.size());
            if (eleccion == 0) {
                return;
            }
            ejecutarConRed(disponibles.get(eleccion - 1));
        }
    }

    private void ejecutarConRed(OpcionMenu opcion) {
        try {
            opcion.accion().run();
        } catch (EntradaCerradaException e) {
            // No es un error: la sesion termino. Sube hasta Main.
            throw e;
        } catch (CrediYaException e) {
            consola.error(e.getMessage());
            Bitacora.advertencia("Operacion rechazada: " + e.getMessage());
        } catch (RuntimeException e) {
            consola.error("Ocurrio un error inesperado. El detalle quedo en datos/bitacora.log");
            Bitacora.error("Error inesperado en la opcion '" + opcion.etiqueta() + "'", e);
        }
        consola.pausa();
    }
}
