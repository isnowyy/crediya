package com.crediya.servicio;

import com.crediya.config.FabricaRepositorios;
import com.crediya.config.Repositorios;
import com.crediya.excepcion.ReglaNegocioException;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.Rol;
import com.crediya.modelo.TipoInteres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas del registro de abonos y del recaudo.
 */
@DisplayName("Servicio de pagos")
class PagoServicioTest {

    private Repositorios repositorios;
    private PagoServicio servicio;
    private Prestamo prestamo;

    @BeforeEach
    void prepararSistema() {
        repositorios = FabricaRepositorios.crearMemoria();

        EmpleadoServicio empleadoServicio = new EmpleadoServicio(repositorios.empleados());
        ClienteServicio clienteServicio = new ClienteServicio(
                repositorios.clientes(), repositorios.prestamos());
        PrestamoServicio prestamoServicio = new PrestamoServicio(
                repositorios.prestamos(), clienteServicio, empleadoServicio);

        servicio = new PagoServicio(repositorios.pagos(), repositorios.prestamos(), prestamoServicio);

        Cliente cliente = clienteServicio.registrar("Pedro Sanchez", "1005432198",
                "pedro@correo.com", "3105558877");
        Empleado empleado = empleadoServicio.registrar("Ana Gomez", "1098765432", Rol.ASESOR,
                "ana@crediya.co", new BigDecimal("3200000"));

        // Total 1.200.000, cuota 120.000
        prestamo = prestamoServicio.crear(cliente.getId(), empleado.getId(),
                new BigDecimal("1000000"), new BigDecimal("2"), 10, TipoInteres.SIMPLE);
    }

    @Test
    @DisplayName("El abono queda guardado y el saldo baja")
    void registraAbono() {
        Pago pago = servicio.registrarAbono(prestamo.getId(), new BigDecimal("120000"));

        assertEquals(1, repositorios.pagos().contar());
        assertEquals(prestamo.getId(), pago.getPrestamoId());

        Prestamo recargado = repositorios.prestamos().buscarPorId(prestamo.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1080000.00").compareTo(recargado.getSaldoPendiente()));
    }

    @Test
    @DisplayName("Al cubrir el total, el prestamo guardado queda en PAGADO")
    void cancelaElPrestamo() {
        servicio.registrarAbono(prestamo.getId(), new BigDecimal("1200000"));

        Prestamo recargado = repositorios.prestamos().buscarPorId(prestamo.getId()).orElseThrow();
        assertEquals(EstadoPrestamo.PAGADO, recargado.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(recargado.getSaldoPendiente()));
    }

    @Test
    @DisplayName("Un abono excesivo se rechaza y no queda registrado")
    void rechazaExceso() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarAbono(prestamo.getId(), new BigDecimal("2000000")));

        assertEquals(0, repositorios.pagos().contar(), "No debio guardarse el pago rechazado");
    }

    @Test
    @DisplayName("El historico sale ordenado del abono mas antiguo al mas reciente")
    void historicoOrdenado() {
        servicio.registrarAbono(prestamo.getId(), new BigDecimal("100000"), LocalDate.now().minusMonths(1));
        servicio.registrarAbono(prestamo.getId(), new BigDecimal("200000"), LocalDate.now().minusMonths(3));

        List<Pago> historico = servicio.historicoDe(prestamo.getId());

        assertEquals(2, historico.size());
        assertEquals(0, new BigDecimal("200000").compareTo(historico.get(0).getMonto()));
    }

    @Test
    @DisplayName("Agrupa el recaudo por mes")
    void agrupaRecaudoPorMes() {
        LocalDate mesPasado = LocalDate.now().minusMonths(1);

        servicio.registrarAbono(prestamo.getId(), new BigDecimal("100000"), mesPasado);
        servicio.registrarAbono(prestamo.getId(), new BigDecimal("150000"), mesPasado);
        servicio.registrarAbono(prestamo.getId(), new BigDecimal("50000"), LocalDate.now());

        Map<YearMonth, BigDecimal> recaudo = servicio.recaudoPorMes();

        assertEquals(2, recaudo.size());
        assertEquals(0, new BigDecimal("250000").compareTo(recaudo.get(YearMonth.from(mesPasado))));
        assertEquals(0, new BigDecimal("300000").compareTo(servicio.totalRecaudado()));
    }
}
