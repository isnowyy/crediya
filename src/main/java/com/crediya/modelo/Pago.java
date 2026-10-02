package com.crediya.modelo;

import com.crediya.excepcion.ValidacionException;
import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Abono que un cliente hace a un prestamo.
 *
 * <p>Un pago es un hecho ya ocurrido: una vez registrado no se edita, solo se
 * consulta. Por eso sus campos son {@code final} salvo el identificador, que lo
 * asigna el almacen al guardarlo.</p>
 */
public class Pago implements Identificable {

    private Integer id;
    private final int prestamoId;
    private final LocalDate fechaPago;
    private final BigDecimal monto;

    public Pago(Integer id, int prestamoId, LocalDate fechaPago, BigDecimal monto) {
        this.id = id;
        this.prestamoId = Validaciones.enteroEnRango(prestamoId, 1, Integer.MAX_VALUE, "prestamoId");
        this.fechaPago = validarFecha(fechaPago);
        this.monto = Validaciones.montoPositivo(monto, "monto del pago");
    }

    /** Pago de hoy que todavia no se ha guardado. */
    public Pago(int prestamoId, BigDecimal monto) {
        this(null, prestamoId, LocalDate.now(), monto);
    }

    private static LocalDate validarFecha(LocalDate fecha) {
        Validaciones.noNulo(fecha, "fecha del pago");
        if (fecha.isAfter(LocalDate.now())) {
            throw new ValidacionException("No se puede registrar un pago con fecha futura: " + fecha);
        }
        return fecha;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public int getPrestamoId() {
        return prestamoId;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Pago pago)) {
            return false;
        }
        return id != null && id.equals(pago.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Pago #%s | prestamo %d | %s | %s",
                id, prestamoId, fechaPago, Dinero.formatear(monto));
    }
}
