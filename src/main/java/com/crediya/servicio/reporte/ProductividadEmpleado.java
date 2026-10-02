package com.crediya.servicio.reporte;

import com.crediya.modelo.Empleado;
import com.crediya.util.Dinero;

import java.math.BigDecimal;

/**
 * Cuanto credito coloco un empleado y que comision le corresponde.
 *
 * @param empleado           funcionario evaluado
 * @param prestamosColocados numero de prestamos que gestiono
 * @param montoColocado      capital total que desembolso
 * @param comision           comision ganada segun el porcentaje de su rol
 */
public record ProductividadEmpleado(Empleado empleado,
                                    long prestamosColocados,
                                    BigDecimal montoColocado,
                                    BigDecimal comision) {

    public String fila() {
        return String.format("%-24s %-22s %2d prestamo(s)  colocado %-18s comision %s",
                empleado.getNombre(), empleado.getRol().getDescripcion(), prestamosColocados,
                Dinero.formatear(montoColocado), Dinero.formatear(comision));
    }
}
