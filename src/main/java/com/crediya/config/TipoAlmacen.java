package com.crediya.config;

import com.crediya.excepcion.ValidacionException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Almacenes disponibles para el sistema.
 *
 * <ul>
 *   <li>{@code MYSQL}   - persistencia real en base de datos por JDBC.</li>
 *   <li>{@code ARCHIVO} - archivos de texto en la carpeta {@code datos/}.</li>
 *   <li>{@code MEMORIA} - solo durante la ejecucion; sirve para demostrar el
 *       sistema sin depender de nada instalado y es el almacen que usan las
 *       pruebas unitarias.</li>
 * </ul>
 */
public enum TipoAlmacen {

    MYSQL("Base de datos MySQL (JDBC)"),
    ARCHIVO("Archivos de texto en datos/"),
    MEMORIA("Memoria volatil (demostracion y pruebas)");

    private final String descripcion;

    TipoAlmacen(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static TipoAlmacen desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("El almacen es obligatorio. Opciones: " + opciones());
        }
        return Arrays.stream(values())
                .filter(tipo -> tipo.name().equalsIgnoreCase(texto.trim()))
                .findFirst()
                .orElseThrow(() -> new ValidacionException(
                        "Almacen desconocido: '" + texto + "'. Opciones: " + opciones()));
    }

    public static String opciones() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
