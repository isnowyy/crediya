package com.crediya.servicio.reporte;

import com.crediya.util.Dinero;

import java.math.BigDecimal;

/**
 * Fotografia de la cartera en un momento dado.
 *
 * @param totalPrestamos  cuantos prestamos existen
 * @param activos         prestamos aun pendientes de pago
 * @param pagados         prestamos ya cancelados
 * @param vencidos        prestamos pendientes que pasaron su fecha de vencimiento
 * @param capitalColocado suma de los capitales desembolsados
 * @param porCobrar       suma de los saldos pendientes
 * @param recaudado       suma de todos los abonos recibidos
 */
public record ResumenCartera(long totalPrestamos,
                             long activos,
                             long pagados,
                             long vencidos,
                             BigDecimal capitalColocado,
                             BigDecimal porCobrar,
                             BigDecimal recaudado) {

    /**
     * Porcentaje de prestamos pendientes que estan vencidos. Es el indicador
     * con el que se mide la salud de una cartera.
     */
    public BigDecimal indiceDeMora() {
        if (activos == 0) {
            return Dinero.cero();
        }
        return Dinero.normalizar(BigDecimal.valueOf(vencidos)
                .multiply(Dinero.CIEN)
                .divide(BigDecimal.valueOf(activos), Dinero.CALCULO));
    }

    public String comoTexto() {
        return """
                Prestamos registrados : %d
                  activos             : %d
                  pagados             : %d
                  vencidos            : %d
                Capital colocado      : %s
                Saldo por cobrar      : %s
                Total recaudado       : %s
                Indice de mora        : %s %%"""
                .formatted(totalPrestamos, activos, pagados, vencidos,
                        Dinero.formatear(capitalColocado),
                        Dinero.formatear(porCobrar),
                        Dinero.formatear(recaudado),
                        indiceDeMora().toPlainString());
    }
}
