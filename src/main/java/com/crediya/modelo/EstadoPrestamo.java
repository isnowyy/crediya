package com.crediya.modelo;

import com.crediya.excepcion.ValidacionException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Estado contable de un prestamo.
 *
 * <p>Son solo dos porque son los unicos que el sistema guarda. "Vencido" y
 * "moroso" no aparecen aqui a proposito: no son estados que alguien escriba,
 * sino conclusiones que se sacan de la fecha de vencimiento y del saldo. Se
 * calculan en {@link Prestamo} y en el modulo de reportes.</p>
 */
public enum EstadoPrestamo {

    PENDIENTE("Pendiente de pago"),
    PAGADO("Cancelado en su totalidad");

    private final String descripcion;

    EstadoPrestamo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static EstadoPrestamo desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("El estado es obligatorio. Opciones: " + opciones());
        }
        return Arrays.stream(values())
                .filter(estado -> estado.name().equalsIgnoreCase(texto.trim()))
                .findFirst()
                .orElseThrow(() -> new ValidacionException(
                        "Estado desconocido: '" + texto + "'. Opciones: " + opciones()));
    }

    public static String opciones() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
