package com.crediya.modelo;

import com.crediya.excepcion.ReglaNegocioException;
import com.crediya.excepcion.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas que el prestamo debe hacer cumplir por si mismo, sin ayuda de ningun
 * servicio: el saldo, el cambio de estado y el rechazo de abonos imposibles.
 */
@DisplayName("Prestamo")
class PrestamoTest {

    private Cliente cliente;
    private Empleado empleado;

    @BeforeEach
    void prepararPersonas() {
        cliente = new Cliente(1, "Pedro Sanchez", "1005432198", "pedro@correo.com", "3105558877");
        empleado = new Empleado(1, "Ana Gomez", "1098765432", Rol.ASESOR,
                "ana@crediya.co", new BigDecimal("3200000"));
    }

    /** Prestamo de 1.000.000 al 2 % a 10 cuotas: total 1.200.000, cuota 120.000. */
    private Prestamo prestamoDePrueba(LocalDate inicio) {
        return Prestamo.builder()
                .id(1)
                .cliente(cliente)
                .empleado(empleado)
                .monto(new BigDecimal("1000000"))
                .tasaInteresMensual(new BigDecimal("2"))
                .cuotas(10)
                .fechaInicio(inicio)
                .tipoInteres(TipoInteres.SIMPLE)
                .construir();
    }

    private static void assertMonto(String esperado, BigDecimal obtenido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtenido),
                () -> "Se esperaba " + esperado + " pero se obtuvo " + obtenido);
    }

    @Test
    @DisplayName("Calcula el monto total y la cuota al construirse")
    void liquidaAlConstruirse() {
        Prestamo prestamo = prestamoDePrueba(LocalDate.now());

        assertMonto("1200000.00", prestamo.getMontoTotal());
        assertMonto("120000.00", prestamo.getValorCuota());
        assertMonto("1200000.00", prestamo.getSaldoPendiente());
        assertEquals(EstadoPrestamo.PENDIENTE, prestamo.getEstado());
    }

    @Test
    @DisplayName("Un abono baja el saldo y suma al total abonado")
    void abonoBajaElSaldo() {
        Prestamo prestamo = prestamoDePrueba(LocalDate.now());

        prestamo.registrarPago(new Pago(null, 1, LocalDate.now(), new BigDecimal("200000")));

        assertMonto("200000.00", prestamo.getTotalAbonado());
        assertMonto("1000000.00", prestamo.getSaldoPendiente());
        assertMonto("16.67", prestamo.getPorcentajePagado());
        assertEquals(EstadoPrestamo.PENDIENTE, prestamo.getEstado());
    }

    @Test
    @DisplayName("Al cubrir el saldo completo pasa solo a PAGADO")
    void sePagaSolo() {
        Prestamo prestamo = prestamoDePrueba(LocalDate.now());

        prestamo.registrarPago(new Pago(null, 1, LocalDate.now(), new BigDecimal("700000")));
        prestamo.registrarPago(new Pago(null, 1, LocalDate.now(), new BigDecimal("500000")));

        assertEquals(EstadoPrestamo.PAGADO, prestamo.getEstado());
        assertMonto("0.00", prestamo.getSaldoPendiente());
    }

    @Test
    @DisplayName("Rechaza un abono mayor que el saldo pendiente")
    void rechazaAbonoExcesivo() {
        Prestamo prestamo = prestamoDePrueba(LocalDate.now());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> prestamo.registrarPago(new Pago(null, 1, LocalDate.now(), new BigDecimal("1200001"))));

        assertTrue(error.getMessage().contains("supera el saldo"));
        assertMonto("1200000.00", prestamo.getSaldoPendiente());
    }

    @Test
    @DisplayName("Un prestamo ya pagado no admite mas abonos")
    void pagadoNoAdmiteAbonos() {
        Prestamo prestamo = prestamoDePrueba(LocalDate.now());
        prestamo.registrarPago(new Pago(null, 1, LocalDate.now(), new BigDecimal("1200000")));

        assertThrows(ReglaNegocioException.class,
                () -> prestamo.registrarPago(new Pago(null, 1, LocalDate.now(), BigDecimal.ONE)));
    }

    @Test
    @DisplayName("Rechaza un abono que pertenece a otro prestamo")
    void rechazaAbonoDeOtroPrestamo() {
        Prestamo prestamo = prestamoDePrueba(LocalDate.now());

        assertThrows(ReglaNegocioException.class,
                () -> prestamo.registrarPago(new Pago(null, 99, LocalDate.now(), new BigDecimal("1000"))));
    }

    @Test
    @DisplayName("Esta vencido cuando paso la fecha y aun hay saldo")
    void detectaVencimiento() {
        LocalDate hoy = LocalDate.of(2026, 10, 1);
        Prestamo prestamo = prestamoDePrueba(hoy.minusMonths(12));

        assertEquals(hoy.minusMonths(2), prestamo.getFechaVencimiento());
        assertTrue(prestamo.estaVencido(hoy));
        assertEquals(61L, prestamo.diasDeMora(hoy));
    }

    @Test
    @DisplayName("Un prestamo pagado nunca figura como vencido")
    void pagadoNoEstaVencido() {
        LocalDate hoy = LocalDate.of(2026, 10, 1);
        Prestamo prestamo = prestamoDePrueba(hoy.minusMonths(12));

        prestamo.registrarPago(new Pago(null, 1, hoy.minusMonths(11), new BigDecimal("1200000")));

        assertFalse(prestamo.estaVencido(hoy));
        assertEquals(0L, prestamo.diasDeMora(hoy));
    }

    @Test
    @DisplayName("No se puede prestar a un cliente que aun no esta registrado")
    void exigeClienteGuardado() {
        Cliente sinId = new Cliente("Nuevo Cliente", "1234567", "nuevo@correo.com", "3001112233");

        assertThrows(ValidacionException.class, () -> Prestamo.builder()
                .cliente(sinId)
                .empleado(empleado)
                .monto(new BigDecimal("1000000"))
                .tasaInteresMensual(new BigDecimal("2"))
                .cuotas(10)
                .construir());
    }

    @Test
    @DisplayName("El historico de pagos no se puede modificar desde afuera")
    void historicoEsDeSoloLectura() {
        Prestamo prestamo = prestamoDePrueba(LocalDate.now());

        assertThrows(UnsupportedOperationException.class,
                () -> prestamo.getPagos().add(new Pago(null, 1, LocalDate.now(), BigDecimal.TEN)));
    }
}
