package com.crediya.modelo;

import com.crediya.excepcion.ValidacionException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Modalidades de credito que ofrece CrediYa.
 *
 * <p>Se guarda en la base de datos junto al prestamo porque sin ella no se
 * podria reconstruir el monto total al volver a leerlo.</p>
 */
public enum TipoInteres {

    SIMPLE("Interes simple sobre el capital"),
    COMPUESTO("Interes compuesto capitalizado mes a mes"),
    CUOTA_FIJA("Amortizacion con cuota fija (sistema frances)");

    private final String descripcion;

    TipoInteres(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static TipoInteres desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("El tipo de interes es obligatorio. Opciones: " + opciones());
        }
        return Arrays.stream(values())
                .filter(tipo -> tipo.name().equalsIgnoreCase(texto.trim()))
                .findFirst()
                .orElseThrow(() -> new ValidacionException(
                        "Tipo de interes desconocido: '" + texto + "'. Opciones: " + opciones()));
    }

    public static String opciones() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
