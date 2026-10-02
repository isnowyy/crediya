package com.crediya.modelo.interes;

import java.math.BigDecimal;

/**
 * Resultado de liquidar un credito: cuanto se debe en total y cuanto se paga
 * cada mes.
 *
 * <p>Es un {@code record} porque es un valor inmutable sin identidad propia;
 * devolver los dos numeros juntos evita tener que calcular dos veces la misma
 * formula.</p>
 *
 * @param montoTotal capital mas intereses
 * @param valorCuota cuota mensual
 */
public record PlanPago(BigDecimal montoTotal, BigDecimal valorCuota) {
}
