package com.crediya.modelo.interes;

import com.crediya.util.Dinero;

import java.math.BigDecimal;

/**
 * Interes simple: los intereses se calculan siempre sobre el capital inicial.
 *
 * <pre>
 *   total = capital * (1 + tasa * cuotas)
 *   cuota = total / cuotas
 * </pre>
 */
public class InteresSimple implements CalculadoraInteres {

    @Override
    public PlanPago calcular(BigDecimal monto, BigDecimal tasaMensual, int cuotas) {
        BigDecimal tasa = Dinero.porcentajeADecimal(tasaMensual);
        BigDecimal factor = BigDecimal.ONE.add(tasa.multiply(BigDecimal.valueOf(cuotas)));

        BigDecimal total = Dinero.normalizar(monto.multiply(factor, Dinero.CALCULO));
        BigDecimal cuota = Dinero.normalizar(total.divide(BigDecimal.valueOf(cuotas), Dinero.CALCULO));

        return new PlanPago(total, cuota);
    }
}
