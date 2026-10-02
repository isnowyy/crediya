package com.crediya.modelo.interes;

import java.math.BigDecimal;

/**
 * Estrategia de liquidacion de un credito (patron Strategy).
 *
 * <p>Cada modalidad de CrediYa implementa esta interfaz. El prestamo no sabe
 * que formula se le aplica, solo pide el plan de pago; agregar una modalidad
 * nueva no obliga a tocar {@code Prestamo} ni los servicios, que es justamente
 * el principio abierto/cerrado.</p>
 */
@FunctionalInterface
public interface CalculadoraInteres {

    /**
     * @param monto        capital prestado, mayor que cero
     * @param tasaMensual  tasa de interes mensual en porcentaje (2.5 significa 2,5 %)
     * @param cuotas       numero de cuotas mensuales, mayor que cero
     * @return monto total a pagar y valor de cada cuota
     */
    PlanPago calcular(BigDecimal monto, BigDecimal tasaMensual, int cuotas);
}
