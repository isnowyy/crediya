package com.crediya.modelo.interes;

import com.crediya.util.Dinero;

import java.math.BigDecimal;

/**
 * Interes compuesto: cada mes los intereses se suman al capital y el mes
 * siguiente tambien generan intereses.
 *
 * <pre>
 *   total = capital * (1 + tasa) ^ cuotas
 *   cuota = total / cuotas
 * </pre>
 */
public class InteresCompuesto implements CalculadoraInteres {

    @Override
    public PlanPago calcular(BigDecimal monto, BigDecimal tasaMensual, int cuotas) {
        BigDecimal tasa = Dinero.porcentajeADecimal(tasaMensual);
        BigDecimal factor = BigDecimal.ONE.add(tasa).pow(cuotas, Dinero.CALCULO);

        BigDecimal total = Dinero.normalizar(monto.multiply(factor, Dinero.CALCULO));
        BigDecimal cuota = Dinero.normalizar(total.divide(BigDecimal.valueOf(cuotas), Dinero.CALCULO));

        return new PlanPago(total, cuota);
    }
}
