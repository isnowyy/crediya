package com.crediya.util;

import com.crediya.excepcion.ValidacionException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Validaciones de entrada reutilizables por todo el dominio.
 *
 * <p>Estan centralizadas aqui para que la misma regla no se reescriba en cada
 * clase: si manana el correo corporativo cambia de formato, se toca un solo
 * metodo.</p>
 */
public final class Validaciones {

    /** Formato practico de correo: algo@algo.algo, sin espacios. */
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$");

    /** Documento colombiano: entre 6 y 15 digitos. */
    private static final Pattern DOCUMENTO = Pattern.compile("^\\d{6,15}$");

    /** Telefono: 7 a 15 digitos, admite el prefijo +57. */
    private static final Pattern TELEFONO = Pattern.compile("^\\+?\\d{7,15}$");

    private Validaciones() {
        // Clase de utilidades: no se instancia.
    }

    public static String textoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("El campo '" + campo + "' es obligatorio.");
        }
        return valor.trim();
    }

    public static String nombre(String valor) {
        String limpio = textoObligatorio(valor, "nombre");
        if (limpio.length() < 3) {
            throw new ValidacionException("El nombre debe tener al menos 3 caracteres.");
        }
        if (limpio.length() > 80) {
            throw new ValidacionException("El nombre no puede superar los 80 caracteres.");
        }
        return limpio;
    }

    public static String documento(String valor) {
        String limpio = textoObligatorio(valor, "documento");
        if (!DOCUMENTO.matcher(limpio).matches()) {
            throw new ValidacionException("El documento debe tener entre 6 y 15 digitos: '" + limpio + "'.");
        }
        return limpio;
    }

    public static String correo(String valor) {
        String limpio = textoObligatorio(valor, "correo").toLowerCase();
        if (!CORREO.matcher(limpio).matches()) {
            throw new ValidacionException("El correo '" + limpio + "' no tiene un formato valido.");
        }
        return limpio;
    }

    public static String telefono(String valor) {
        String limpio = textoObligatorio(valor, "telefono").replace(" ", "");
        if (!TELEFONO.matcher(limpio).matches()) {
            throw new ValidacionException("El telefono '" + limpio + "' no tiene un formato valido.");
        }
        return limpio;
    }

    public static BigDecimal montoPositivo(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new ValidacionException("El campo '" + campo + "' es obligatorio.");
        }
        if (valor.signum() <= 0) {
            throw new ValidacionException("El campo '" + campo + "' debe ser mayor que cero.");
        }
        return Dinero.normalizar(valor);
    }

    public static BigDecimal porcentaje(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new ValidacionException("El campo '" + campo + "' es obligatorio.");
        }
        if (valor.signum() < 0 || valor.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidacionException("El campo '" + campo + "' debe estar entre 0 y 100.");
        }
        return valor;
    }

    public static int enteroEnRango(int valor, int minimo, int maximo, String campo) {
        if (valor < minimo || valor > maximo) {
            throw new ValidacionException(
                    "El campo '" + campo + "' debe estar entre " + minimo + " y " + maximo + ".");
        }
        return valor;
    }

    public static <T> T noNulo(T valor, String campo) {
        if (valor == null) {
            throw new ValidacionException("El campo '" + campo + "' es obligatorio.");
        }
        return valor;
    }
}
