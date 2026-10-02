package com.crediya.servicio;

import com.crediya.config.FabricaRepositorios;
import com.crediya.config.Repositorios;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.Rol;
import com.crediya.modelo.TipoInteres;
import com.crediya.servicio.reporte.ClienteMoroso;
import com.crediya.servicio.reporte.ProductividadEmpleado;
import com.crediya.servicio.reporte.ResumenCartera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del modulo de reportes.
 *
 * <p>Todas fijan la fecha de referencia en lugar de usar el reloj del sistema.
 * Una prueba que dependa de "hoy" funciona el dia que se escribe y falla sola
 * meses despues; fijar la fecha es lo que la vuelve repetible.</p>
 */
@DisplayName("Servicio de reportes")
class ReporteServicioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private Repositorios repositorios;
    private ReporteServicio servicio;
    private Cliente alDia;
    private Cliente moroso;
    private Empleado asesor;
    private Empleado cobrador;

    @BeforeEach
    void prepararCartera() {
        repositorios = FabricaRepositorios.crearMemoria();
        servicio = new ReporteServicio(
                repositorios.prestamos(), repositorios.pagos(), repositorios.empleados());

        alDia = repositorios.clientes().guardar(
                new Cliente("Pedro Sanchez", "1005432198", "pedro@correo.com", "3105558877"));
        moroso = repositorios.clientes().guardar(
                new Cliente("Marta Quintero", "63301122", "marta@correo.com", "3009991122"));

        asesor = repositorios.empleados().guardar(new Empleado("Ana Gomez", "1098765432",
                Rol.ASESOR, "ana@crediya.co", new BigDecimal("3200000")));
        cobrador = repositorios.empleados().guardar(new Empleado("Carlos Pena", "91234567",
                Rol.COBRADOR, "carlos@crediya.co", new BigDecimal("2400000")));

        // Vigente: empezo hace un mes, vence en nueve.
        crearPrestamo(alDia, asesor, "1000000", 10, HOY.minusMonths(1));

        // Vencido: empezo hace dos anios con plazo de seis meses.
        crearPrestamo(moroso, cobrador, "500000", 6, HOY.minusYears(2));
    }

    private Prestamo crearPrestamo(Cliente cliente, Empleado empleado,
                                   String monto, int cuotas, LocalDate inicio) {
        return repositorios.prestamos().guardar(Prestamo.builder()
                .cliente(cliente)
                .empleado(empleado)
                .monto(new BigDecimal(monto))
                .tasaInteresMensual(new BigDecimal("2"))
                .cuotas(cuotas)
                .fechaInicio(inicio)
                .tipoInteres(TipoInteres.SIMPLE)
                .construir());
    }

    @Test
    @DisplayName("Lista como activos los dos prestamos pendientes")
    void listaActivos() {
        assertEquals(2, servicio.prestamosActivos().size());
    }

    @Test
    @DisplayName("Solo el prestamo fuera de plazo figura como vencido")
    void listaVencidos() {
        List<Prestamo> vencidos = servicio.prestamosVencidos(HOY);

        assertEquals(1, vencidos.size());
        assertEquals(moroso, vencidos.get(0).getCliente());
    }

    @Test
    @DisplayName("Identifica al cliente moroso con su deuda y sus dias de mora")
    void identificaMorosos() {
        List<ClienteMoroso> morosos = servicio.clientesMorosos(HOY);

        assertEquals(1, morosos.size());

        ClienteMoroso resultado = morosos.get(0);
        assertEquals(moroso, resultado.cliente());
        assertEquals(1, resultado.prestamosVencidos());
        // 500.000 * (1 + 0,02 * 6) = 560.000
        assertEquals(0, new BigDecimal("560000.00").compareTo(resultado.saldoTotal()));
        assertTrue(resultado.diasMaximoMora() > 500, "Lleva mas de 18 meses vencido");
    }

    @Test
    @DisplayName("Un cliente al dia no aparece en el reporte de morosos")
    void alDiaNoEsMoroso() {
        assertTrue(servicio.clientesMorosos(HOY).stream()
                .noneMatch(resultado -> resultado.cliente().equals(alDia)));
    }

    @Test
    @DisplayName("El resumen cuadra capital, saldo y recaudo")
    void resumeLaCartera() {
        repositorios.pagos().guardar(new Pago(null, 1, HOY.minusDays(10), new BigDecimal("120000")));

        ResumenCartera resumen = servicio.resumenCartera(HOY);

        assertEquals(2, resumen.totalPrestamos());
        assertEquals(2, resumen.activos());
        assertEquals(1, resumen.vencidos());
        assertEquals(0, new BigDecimal("1500000.00").compareTo(resumen.capitalColocado()));
        assertEquals(0, new BigDecimal("120000.00").compareTo(resumen.recaudado()));
        assertEquals(0, new BigDecimal("50.00").compareTo(resumen.indiceDeMora()));
    }

    @Test
    @DisplayName("Calcula la comision de cada empleado segun su rol")
    void calculaComisiones() {
        List<ProductividadEmpleado> productividad = servicio.productividadPorEmpleado();

        assertEquals(2, productividad.size());

        ProductividadEmpleado primero = productividad.get(0);
        assertEquals(asesor, primero.empleado());
        // 1.000.000 al 1,50 % que gana un asesor
        assertEquals(0, new BigDecimal("15000.00").compareTo(primero.comision()));

        ProductividadEmpleado segundo = productividad.get(1);
        assertEquals(cobrador, segundo.empleado());
        // 500.000 al 0,80 % que gana un cobrador
        assertEquals(0, new BigDecimal("4000.00").compareTo(segundo.comision()));
    }

    @Test
    @DisplayName("El filtro generico por lambda aplica la condicion recibida")
    void filtraConLambda() {
        List<Prestamo> grandes = servicio.buscar(
                prestamo -> prestamo.getMonto().compareTo(new BigDecimal("600000")) > 0);

        assertEquals(1, grandes.size());
        assertEquals(alDia, grandes.get(0).getCliente());
    }

    @Test
    @DisplayName("Filtra por rango de monto")
    void filtraPorRango() {
        assertEquals(2, servicio.prestamosEntre(
                new BigDecimal("100000"), new BigDecimal("2000000")).size());
        assertEquals(0, servicio.prestamosEntre(
                new BigDecimal("2000000"), new BigDecimal("3000000")).size());
    }
}
