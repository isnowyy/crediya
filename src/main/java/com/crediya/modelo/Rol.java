package com.crediya.modelo;

import com.crediya.excepcion.ValidacionException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.crediya.util.Dinero;

/**
 * Roles que puede ocupar un empleado y comision que gana por cada prestamo
 * que coloca.
 *
 * <p>La comision vive dentro del enum y no en una cadena de {@code if}
 * repartida por el codigo: agregar un rol nuevo es agregar una constante y nada
 * mas.</p>
 */
public enum Rol {

    ASESOR("Asesor comercial", new BigDecimal("1.50")),
    COBRADOR("Gestor de cobranza", new BigDecimal("0.80")),
    ANALISTA("Analista de credito", new BigDecimal("0.50")),
    ADMINISTRADOR("Administrador de oficina", BigDecimal.ZERO);

    private final String descripcion;
    private final BigDecimal porcentajeComision;

    Rol(String descripcion, BigDecimal porcentajeComision) {
        this.descripcion = descripcion;
        this.porcentajeComision = porcentajeComision;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPorcentajeComision() {
        return porcentajeComision;
    }

    /** Comision que le corresponde al rol sobre un monto colocado. */
    public BigDecimal comisionSobre(BigDecimal monto) {
        if (monto == null) {
            return Dinero.cero();
        }
        return Dinero.normalizar(monto.multiply(Dinero.porcentajeADecimal(porcentajeComision)));
    }

    /**
     * Convierte texto a rol sin distinguir mayusculas y con un mensaje de error
     * que dice cuales son los valores aceptados.
     */
    public static Rol desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("El rol es obligatorio. Opciones: " + opciones());
        }
        return Arrays.stream(values())
                .filter(rol -> rol.name().equalsIgnoreCase(texto.trim()))
                .findFirst()
                .orElseThrow(() -> new ValidacionException(
                        "Rol desconocido: '" + texto + "'. Opciones: " + opciones()));
    }

    public static String opciones() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
