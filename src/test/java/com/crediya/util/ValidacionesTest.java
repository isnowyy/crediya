package com.crediya.util;

import com.crediya.excepcion.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de las validaciones de entrada.
 *
 * <p>Las entradas invalidas se prueban con {@code @ParameterizedTest}: una sola
 * prueba cubre muchos casos y agregar uno nuevo es agregar una linea.</p>
 */
@DisplayName("Validaciones de entrada")
class ValidacionesTest {

    @Test
    @DisplayName("Un nombre valido se recorta y se acepta")
    void aceptaNombreValido() {
        assertEquals("Ana Gomez", Validaciones.nombre("  Ana Gomez  "));
    }

    @ParameterizedTest(name = "rechaza el nombre \"{0}\"")
    @ValueSource(strings = {"", "   ", "Jo"})
    @DisplayName("Rechaza nombres vacios o demasiado cortos")
    void rechazaNombresInvalidos(String nombre) {
        assertThrows(ValidacionException.class, () -> Validaciones.nombre(nombre));
    }

    @Test
    @DisplayName("El correo se guarda en minusculas")
    void normalizaElCorreo() {
        assertEquals("ana.gomez@crediya.co", Validaciones.correo("Ana.Gomez@CrediYa.CO"));
    }

    @ParameterizedTest(name = "rechaza el correo \"{0}\"")
    @ValueSource(strings = {"sinarroba.com", "@sinusuario.com", "falta@dominio", "con espacio@a.com"})
    @DisplayName("Rechaza correos mal formados")
    void rechazaCorreosInvalidos(String correo) {
        assertThrows(ValidacionException.class, () -> Validaciones.correo(correo));
    }

    @ParameterizedTest(name = "rechaza el documento \"{0}\"")
    @ValueSource(strings = {"12345", "abcdefgh", "1234567890123456"})
    @DisplayName("El documento debe tener entre 6 y 15 digitos")
    void rechazaDocumentosInvalidos(String documento) {
        assertThrows(ValidacionException.class, () -> Validaciones.documento(documento));
    }

    @Test
    @DisplayName("Un monto se normaliza a dos decimales")
    void normalizaElMonto() {
        assertEquals(0, new BigDecimal("1000.00")
                .compareTo(Validaciones.montoPositivo(new BigDecimal("1000"), "monto")));
    }

    @Test
    @DisplayName("Un monto de cero o negativo no es valido")
    void rechazaMontoNoPositivo() {
        assertThrows(ValidacionException.class,
                () -> Validaciones.montoPositivo(BigDecimal.ZERO, "monto"));
        assertThrows(ValidacionException.class,
                () -> Validaciones.montoPositivo(new BigDecimal("-1"), "monto"));
    }

    @Test
    @DisplayName("El porcentaje debe estar entre 0 y 100")
    void validaElPorcentaje() {
        assertEquals(0, new BigDecimal("2.5")
                .compareTo(Validaciones.porcentaje(new BigDecimal("2.5"), "interes")));
        assertThrows(ValidacionException.class,
                () -> Validaciones.porcentaje(new BigDecimal("101"), "interes"));
    }
}
