package com.crediya.repositorio.archivo;

import com.crediya.excepcion.PersistenciaException;
import com.crediya.modelo.Identificable;
import com.crediya.repositorio.Repositorio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base de todos los repositorios que guardan en archivos de texto plano
 * (patron Template Method).
 *
 * <p>Aqui vive todo lo que es igual para empleados, clientes, prestamos y
 * pagos: abrir el archivo, saltarse la cabecera, partir las lineas, numerar las
 * entidades nuevas y volver a escribir. Lo unico que cambia de una entidad a
 * otra son tres pasos, que se dejan abstractos para que cada subclase los
 * complete. Sin esta clase, las cuatro implementaciones serian el mismo codigo
 * copiado cuatro veces.</p>
 *
 * <p>Formato: valores separados por punto y coma, codificacion UTF-8 y una
 * primera linea de comentario que documenta las columnas. Es un formato que se
 * puede abrir en Excel y revisar a ojo durante la sustentacion.</p>
 *
 * <p>Nota de alcance: cada operacion lee y reescribe el archivo completo. Para
 * los volumenes de este ejercicio es intrascendente y mantiene el codigo
 * legible; un sistema real usaria la base de datos, que es justamente la otra
 * implementacion disponible.</p>
 *
 * @param <T> tipo de entidad administrada
 */
public abstract class RepositorioArchivo<T extends Identificable> implements Repositorio<T> {

    protected static final String SEPARADOR = ";";
    private static final String MARCA_COMENTARIO = "#";

    private final Path archivo;

    protected RepositorioArchivo(Path archivo) {
        this.archivo = archivo;
        prepararArchivo();
    }

    // ------------------------------------------------------------------
    // Pasos que cada subclase completa
    // ------------------------------------------------------------------

    /** Linea de comentario con el nombre de las columnas. */
    protected abstract String cabecera();

    /** Convierte la entidad en una linea del archivo. */
    protected abstract String serializar(T entidad);

    /**
     * Reconstruye la entidad a partir de los campos de una linea.
     *
     * @param campos valores ya separados, en el mismo orden de {@link #cabecera()}
     */
    protected abstract T deserializar(String[] campos);

    // ------------------------------------------------------------------
    // Algoritmo comun
    // ------------------------------------------------------------------

    @Override
    public T guardar(T entidad) {
        List<T> entidades = listar();

        if (entidad.esNueva()) {
            entidad.setId(siguienteId(entidades));
            entidades.add(entidad);
        } else {
            int posicion = indiceDe(entidades, entidad.getId());
            if (posicion >= 0) {
                entidades.set(posicion, entidad);
            } else {
                entidades.add(entidad);
            }
        }

        escribirTodo(entidades);
        return entidad;
    }

    @Override
    public Optional<T> buscarPorId(int id) {
        return listar().stream()
                .filter(entidad -> Integer.valueOf(id).equals(entidad.getId()))
                .findFirst();
    }

    @Override
    public List<T> listar() {
        try (var lineas = Files.lines(archivo, StandardCharsets.UTF_8)) {
            List<T> entidades = new ArrayList<>();
            lineas.map(String::trim)
                    .filter(linea -> !linea.isEmpty() && !linea.startsWith(MARCA_COMENTARIO))
                    .forEach(linea -> entidades.add(deserializar(linea.split(SEPARADOR, -1))));
            return entidades;
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el archivo " + archivo, e);
        } catch (RuntimeException e) {
            throw new PersistenciaException("El archivo " + archivo + " tiene una linea con formato invalido.", e);
        }
    }

    @Override
    public boolean eliminar(int id) {
        List<T> entidades = listar();
        boolean removido = entidades.removeIf(entidad -> Integer.valueOf(id).equals(entidad.getId()));
        if (removido) {
            escribirTodo(entidades);
        }
        return removido;
    }

    // ------------------------------------------------------------------
    // Apoyo para las subclases
    // ------------------------------------------------------------------

    /**
     * Deja el valor listo para escribirse: sin separadores ni saltos de linea
     * que partirian la fila en dos.
     */
    protected static String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace(SEPARADOR, ",").replace("\n", " ").replace("\r", " ").trim();
    }

    /** Lee un campo opcional: devuelve {@code null} cuando viene vacio. */
    protected static String campoOpcional(String[] campos, int indice) {
        if (indice >= campos.length) {
            return null;
        }
        String valor = campos[indice].trim();
        return valor.isEmpty() ? null : valor;
    }

    protected Path getArchivo() {
        return archivo;
    }

    // ------------------------------------------------------------------
    // Detalles internos
    // ------------------------------------------------------------------

    private void prepararArchivo() {
        try {
            Path carpeta = archivo.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            if (Files.notExists(archivo)) {
                Files.writeString(archivo, MARCA_COMENTARIO + " " + cabecera() + System.lineSeparator(),
                        StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo preparar el archivo " + archivo, e);
        }
    }

    private void escribirTodo(List<T> entidades) {
        StringBuilder contenido = new StringBuilder()
                .append(MARCA_COMENTARIO).append(' ').append(cabecera()).append(System.lineSeparator());

        entidades.forEach(entidad -> contenido.append(serializar(entidad)).append(System.lineSeparator()));

        try {
            Files.writeString(archivo, contenido.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo escribir el archivo " + archivo, e);
        }
    }

    private int siguienteId(List<T> entidades) {
        return entidades.stream()
                .map(Identificable::getId)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private int indiceDe(List<T> entidades, Integer id) {
        for (int i = 0; i < entidades.size(); i++) {
            if (id.equals(entidades.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
}
