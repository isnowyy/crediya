package com.crediya.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Registro de lo que hace el sistema, en {@code datos/bitacora.log}.
 *
 * <p>Se usa {@code java.util.logging} y no {@code System.out} porque la consola
 * ya esta ocupada por los menus: mezclar errores tecnicos con el dialogo del
 * usuario hace ilegibles los dos. En la bitacora quedan las trazas completas de
 * las excepciones; al usuario solo se le muestra el mensaje.</p>
 *
 * <p>Si el archivo no se puede crear (por ejemplo, carpeta sin permisos), el
 * sistema sigue funcionando: registrar no es una funcion critica.</p>
 */
public final class Bitacora {

    private static final Logger LOGGER = Logger.getLogger("crediya");

    static {
        configurar();
    }

    private Bitacora() {
        // Clase de utilidades: no se instancia.
    }

    public static void info(String mensaje) {
        LOGGER.info(mensaje);
    }

    public static void advertencia(String mensaje) {
        LOGGER.warning(mensaje);
    }

    public static void error(String mensaje, Throwable causa) {
        LOGGER.log(Level.SEVERE, mensaje, causa);
    }

    private static void configurar() {
        try {
            Path carpeta = Path.of("datos");
            Files.createDirectories(carpeta);

            FileHandler manejador = new FileHandler(carpeta.resolve("bitacora.log").toString(), true);
            manejador.setFormatter(new SimpleFormatter());

            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(manejador);
            LOGGER.setLevel(Level.INFO);
        } catch (IOException e) {
            // Sin bitacora en archivo el programa sigue: solo se pierde la traza.
            LOGGER.setLevel(Level.OFF);
        }
    }
}
