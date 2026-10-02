package com.crediya.modelo.interes;

import com.crediya.modelo.TipoInteres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica las tres formulas de liquidacion contra valores calculados a mano.
 *
 * <p>Se comparan los montos con {@code compareTo} y no con {@code equals}:
 * para {@link BigDecimal}, {@code 100.0} y {@code 100.00} son distintos como
 * objetos pero iguales como cantidad, y aqui lo que importa es la cantidad.</p>
 */
@DisplayName("Calculadoras de interes")
class CalculadoraInteresTest {

    private static void assertMonto(String esperado, BigDecimal obtenido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtenido),
                () -> "Se esperaba " + esperado + " pero se obtuvo " + obtenido);
    }

    @Nested
    @DisplayName("Interes simple")
    class Simple {

        private final CalculadoraInteres calculadora = new InteresSimple();

        @Test
        @DisplayName("1.000.000 al 2 % mensual a 12 cuotas da 1.240.000")
        void calculaMontoTotal() {
            PlanPago plan = calculadora.calcular(
                    new BigDecimal("1000000"), new BigDecimal("2"), 12);

            // 1.000.000 * (1 + 0,02 * 12) = 1.240.000
            assertMonto("1240000.00", plan.montoTotal());
            assertMonto("103333.33", plan.valorCuota());
        }

        @Test
        @DisplayName("Sin interes, el total es el capital")
        void sinInteres() {
            PlanPago plan = calculadora.calcular(
                    new BigDecimal("600000"), BigDecimal.ZERO, 6);

            assertMonto("600000.00", plan.montoTotal());
            assertMonto("100000.00", plan.valorCuota());
        }
    }

    @Nested
    @DisplayName("Interes compuesto")
    class Compuesto {

        private final CalculadoraInteres calculadora = new InteresCompuesto();

        @Test
        @DisplayName("1.500.000 al 3 % mensual a 4 cuotas da 1.688.263,22")
        void capitalizaMesAMes() {
            PlanPago plan = calculadora.calcular(
                    new BigDecimal("1500000"), new BigDecimal("3"), 4);

            // 1.500.000 * 1,03^4 = 1.688.263,215
            assertMonto("1688263.22", plan.montoTotal());
        }

        @Test
        @DisplayName("Cobra mas que el interes simple a igual tasa y plazo")
        void superaAlSimple() {
            BigDecimal capital = new BigDecimal("1000000");
            BigDecimal tasa = new BigDecimal("2");

            BigDecimal compuesto = calculadora.calcular(capital, tasa, 12).montoTotal();
            BigDecimal simple = new InteresSimple().calcular(capital, tasa, 12).montoTotal();

            assertTrue(compuesto.compareTo(simple) > 0,
                    "El interes compuesto debe superar al simple en 12 meses");
        }
    }

    @Nested
    @DisplayName("Cuota fija (sistema frances)")
    class Frances {

        private final CalculadoraInteres calculadora = new CuotaFija();

        @Test
        @DisplayName("10.000.000 al 1,5 % mensual a 12 cuotas da una cuota de 916.799,93")
        void calculaCuotaConstante() {
            PlanPago plan = calculadora.calcular(
                    new BigDecimal("10000000"), new BigDecimal("1.5"), 12);

            // c = 10.000.000 * 0,015 * 1,015^12 / (1,015^12 - 1)
            assertMonto("916799.93", plan.valorCuota());
            assertMonto("11001599.16", plan.montoTotal());
        }

        @Test
        @DisplayName("Con tasa cero reparte el capital sin dividir por cero")
        void tasaCeroNoRompe() {
            PlanPago plan = calculadora.calcular(
                    new BigDecimal("1200000"), BigDecimal.ZERO, 12);

            assertMonto("100000.00", plan.valorCuota());
            assertMonto("1200000.00", plan.montoTotal());
        }
    }

    @Nested
    @DisplayName("Fabrica de calculadoras")
    class Fabrica {

        @Test
        @DisplayName("Entrega la estrategia que corresponde a cada modalidad")
        void entregaLaEstrategiaCorrecta() {
            assertTrue(CalculadoraInteresFactory.para(TipoInteres.SIMPLE) instanceof InteresSimple);
            assertTrue(CalculadoraInteresFactory.para(TipoInteres.COMPUESTO) instanceof InteresCompuesto);
            assertTrue(CalculadoraInteresFactory.para(TipoInteres.CUOTA_FIJA) instanceof CuotaFija);
        }

        @Test
        @DisplayName("Reutiliza la misma instancia porque no guardan estado")
        void comparteInstancias() {
            assertSame(CalculadoraInteresFactory.para(TipoInteres.SIMPLE),
                    CalculadoraInteresFactory.para(TipoInteres.SIMPLE));
        }
    }
}
