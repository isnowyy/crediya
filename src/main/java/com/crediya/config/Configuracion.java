package com.crediya.config;

import com.crediya.excepcion.PersistenciaException;
import com.crediya.util.Bitacora;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Parametros de arranque del sistema.
 *
 * <p>Los valores se buscan en tres sitios y gana el primero que los tenga:</p>
 * <ol>
 *   <li>variables de entorno, por ejemplo {@code CREDIYA_DB_CLAVE};</li>
 *   <li>{@code crediya.local.properties} en la raiz del proyecto, ignorado por git;</li>
 *   <li>{@code crediya.properties} empaquetado en el jar.</li>
 * </ol>
 *
 * <p>El orden no es casual: asi la clave real nunca queda en el repositorio y
 * el mismo jar sirve para la maquina del estudiante y para la del profesor sin
 * recompilar nada.</p>
 */
public final class Configuracion {

    private static final String ARCHIVO_EMPAQUETADO = "crediya.properties";
    private static final Path ARCHIVO_LOCAL = Path.of("crediya.local.properties");

    private final Properties propiedades = new Properties();

    private Configuracion() {
        cargarEmpaquetado();
        cargarLocal();
    }

    /**
     * Instancia unica, creada la primera vez que alguien la pide.
     *
     * <p>Se usa el modismo de la clase interna porque la JVM garantiza que una
     * clase se inicializa una sola vez y de forma segura entre hilos, sin
     * necesidad de bloques {@code synchronized}.</p>
     */
    private static final class Contenedor {
        private static final Configuracion INSTANCIA = new Configuracion();
    }

    public static Configuracion getInstancia() {
        return Contenedor.INSTANCIA;
    }

    /**
     * @param clave clave con puntos, por ejemplo {@code crediya.db.url}
     */
    public String obtener(String clave, String porDefecto) {
        String desdeEntorno = System.getenv(aVariableDeEntorno(clave));
        if (desdeEntorno != null && !desdeEntorno.isBlank()) {
            return desdeEntorno.trim();
        }
        return propiedades.getProperty(clave, porDefecto);
    }

    public String obtenerObligatorio(String clave) {
        String valor = obtener(clave, null);
        if (valor == null || valor.isBlank()) {
            throw new PersistenciaException("Falta la propiedad de configuracion '" + clave + "'.");
        }
        return valor;
    }

    public Path carpetaDatos() {
        return Path.of(obtener("crediya.datos.carpeta", "datos"));
    }

    public TipoAlmacen almacen() {
        return TipoAlmacen.desde(obtener("crediya.persistencia", TipoAlmacen.ARCHIVO.name()));
    }

    /** {@code crediya.db.url} se convierte en {@code CREDIYA_DB_URL}. */
    private static String aVariableDeEntorno(String clave) {
        return clave.replace('.', '_').toUpperCase();
    }

    private void cargarEmpaquetado() {
        try (InputStream entrada = Configuracion.class.getClassLoader().getResourceAsStream(ARCHIVO_EMPAQUETADO)) {
            if (entrada != null) {
                propiedades.load(entrada);
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer " + ARCHIVO_EMPAQUETADO, e);
        }
    }

    private void cargarLocal() {
        if (Files.notExists(ARCHIVO_LOCAL)) {
            return;
        }
        try (Reader lector = Files.newBufferedReader(ARCHIVO_LOCAL, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
            Bitacora.info("Configuracion local aplicada desde " + ARCHIVO_LOCAL.toAbsolutePath());
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer " + ARCHIVO_LOCAL, e);
        }
    }
}
