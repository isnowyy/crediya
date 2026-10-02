package com.crediya.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Reglas unicas para manejar dinero en el sistema.
 *
 * <p>Todo el proyecto usa {@link BigDecimal} y nunca {@code double}: con punto
 * flotante, {@code 0.1 + 0.2} no da {@code 0.3} y una cartera terminaria
 * descuadrada por centavos. Los montos se guardan siempre con dos decimales y
 * redondeo HALF_UP, que es la convencion contable.</p>
 */
public final class Dinero {

    /** Decimales de cualquier valor monetario. */
    public static final int ESCALA = 2;

    /** Redondeo contable: 0.5 sube. */
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /** Precision intermedia para potencias y divisiones de las formulas de interes. */
    public static final MathContext CALCULO = new MathContext(16, RoundingMode.HALF_UP);

    public static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private Dinero() {
        // Clase de utilidades: no se instancia.
    }

    /** Lleva cualquier valor a la escala monetaria del sistema. */
    public static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(ESCALA) : valor.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA);
    }

    /** Convierte una tasa en porcentaje (2.5) a su forma decimal (0.025). */
    public static BigDecimal porcentajeADecimal(BigDecimal porcentaje) {
        return porcentaje.divide(CIEN, CALCULO);
    }

    /** Formatea como moneda colombiana: {@code $ 1.250.000,00}. */
    public static String formatear(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(COLOMBIA).format(normalizar(valor));
    }
}
