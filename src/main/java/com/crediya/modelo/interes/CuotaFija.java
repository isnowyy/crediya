package com.crediya.modelo.interes;

import com.crediya.util.Dinero;

import java.math.BigDecimal;

/**
 * Sistema frances de amortizacion: la cuota es igual todos los meses y dentro
 * de ella va cambiando la proporcion entre intereses y abono a capital. Es la
 * modalidad que usan los creditos de consumo reales.
 *
 * <pre>
 *   cuota = capital * tasa * (1 + tasa)^n / ((1 + tasa)^n - 1)
 *   total = cuota * n
 * </pre>
 *
 * <p>Cuando la tasa es cero la formula se indetermina (division por cero), asi
 * que ese caso se resuelve aparte repartiendo el capital entre las cuotas.</p>
 */
public class CuotaFija implements CalculadoraInteres {

    @Override
    public PlanPago calcular(BigDecimal monto, BigDecimal tasaMensual, int cuotas) {
        BigDecimal numeroCuotas = BigDecimal.valueOf(cuotas);
        BigDecimal tasa = Dinero.porcentajeADecimal(tasaMensual);

        if (tasa.signum() == 0) {
            BigDecimal cuotaSinInteres = Dinero.normalizar(monto.divide(numeroCuotas, Dinero.CALCULO));
            return new PlanPago(Dinero.normalizar(cuotaSinInteres.multiply(numeroCuotas)), cuotaSinInteres);
        }

        BigDecimal factor = BigDecimal.ONE.add(tasa).pow(cuotas, Dinero.CALCULO);
        BigDecimal cuota = monto.multiply(tasa, Dinero.CALCULO)
                .multiply(factor, Dinero.CALCULO)
                .divide(factor.subtract(BigDecimal.ONE), Dinero.CALCULO);

        BigDecimal cuotaFinal = Dinero.normalizar(cuota);
        return new PlanPago(Dinero.normalizar(cuotaFinal.multiply(numeroCuotas)), cuotaFinal);
    }
}
