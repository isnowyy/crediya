package com.crediya.modelo.interes;

import com.crediya.modelo.TipoInteres;

import java.util.EnumMap;
import java.util.Map;

/**
 * Fabrica que entrega la estrategia de calculo asociada a cada modalidad
 * (patron Factory).
 *
 * <p>Existe para que el enum {@link TipoInteres} siga siendo un dato plano y no
 * cargue con la logica de las formulas, y para que quien necesite calcular no
 * tenga que instanciar la clase concreta. Las estrategias no guardan estado, de
 * modo que se comparte una sola instancia de cada una.</p>
 */
public final class CalculadoraInteresFactory {

    private static final Map<TipoInteres, CalculadoraInteres> ESTRATEGIAS = new EnumMap<>(TipoInteres.class);

    static {
        ESTRATEGIAS.put(TipoInteres.SIMPLE, new InteresSimple());
        ESTRATEGIAS.put(TipoInteres.COMPUESTO, new InteresCompuesto());
        ESTRATEGIAS.put(TipoInteres.CUOTA_FIJA, new CuotaFija());
    }

    private CalculadoraInteresFactory() {
        // Fabrica estatica: no se instancia.
    }

    public static CalculadoraInteres para(TipoInteres tipo) {
        CalculadoraInteres calculadora = ESTRATEGIAS.get(tipo);
        if (calculadora == null) {
            throw new IllegalStateException("No hay calculadora registrada para el tipo " + tipo);
        }
        return calculadora;
    }
}
