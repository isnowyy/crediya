package com.crediya.servicio;

import com.crediya.config.FabricaRepositorios;
import com.crediya.config.Repositorios;
import com.crediya.excepcion.RecursoNoEncontradoException;
import com.crediya.excepcion.ReglaNegocioException;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.Rol;
import com.crediya.modelo.TipoInteres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del servicio de prestamos.
 *
 * <p>Usan el almacen en memoria. Que estas pruebas puedan correr sin MySQL ni
 * archivos es la demostracion practica de que el servicio depende de la
 * interfaz {@code PrestamoRepositorio} y no de una tecnologia concreta.</p>
 */
@DisplayName("Servicio de prestamos")
class PrestamoServicioTest {

    private Repositorios repositorios;
    private PrestamoServicio servicio;
    private Cliente cliente;
    private Empleado empleado;

    @BeforeEach
    void prepararSistema() {
        repositorios = FabricaRepositorios.crearMemoria();

        EmpleadoServicio empleadoServicio = new EmpleadoServicio(repositorios.empleados());
        ClienteServicio clienteServicio = new ClienteServicio(
                repositorios.clientes(), repositorios.prestamos());
        servicio = new PrestamoServicio(repositorios.prestamos(), clienteServicio, empleadoServicio);

        cliente = clienteServicio.registrar("Pedro Sanchez", "1005432198",
                "pedro@correo.com", "3105558877");
        empleado = empleadoServicio.registrar("Ana Gomez", "1098765432", Rol.ASESOR,
                "ana@crediya.co", new BigDecimal("3200000"));
    }

    @Test
    @DisplayName("Crea el prestamo y lo deja guardado con su identificador")
    void creaYGuarda() {
        Prestamo prestamo = servicio.crear(cliente.getId(), empleado.getId(),
                new BigDecimal("1000000"), new BigDecimal("2"), 10, TipoInteres.SIMPLE);

        assertEquals(1, repositorios.prestamos().contar());
        assertEquals(0, new BigDecimal("1200000.00").compareTo(prestamo.getMontoTotal()));
        assertEquals(EstadoPrestamo.PENDIENTE, prestamo.getEstado());
    }

    @Test
    @DisplayName("Falla con un mensaje claro si el cliente no existe")
    void exigeClienteExistente() {
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(999, empleado.getId(),
                        new BigDecimal("1000000"), new BigDecimal("2"), 10, TipoInteres.SIMPLE));

        assertTrue(error.getMessage().contains("Cliente"));
    }

    @Test
    @DisplayName("Niega credito nuevo a un cliente con cartera vencida")
    void niegaCreditoAMoroso() {
        // Prestamo de hace dos anios a 6 cuotas: lleva mas de un ano vencido.
        repositorios.prestamos().guardar(Prestamo.builder()
                .cliente(cliente)
                .empleado(empleado)
                .monto(new BigDecimal("500000"))
                .tasaInteresMensual(new BigDecimal("2"))
                .cuotas(6)
                .fechaInicio(LocalDate.now().minusYears(2))
                .tipoInteres(TipoInteres.SIMPLE)
                .construir());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(cliente.getId(), empleado.getId(),
                        new BigDecimal("1000000"), new BigDecimal("2"), 10, TipoInteres.SIMPLE));

        assertTrue(error.getMessage().contains("vencido"));
        assertEquals(1, repositorios.prestamos().contar(), "No debio crearse el segundo prestamo");
    }

    @Test
    @DisplayName("La simulacion no guarda nada")
    void simularNoPersiste() {
        var plan = servicio.simular(new BigDecimal("1000000"), new BigDecimal("2"), 10, TipoInteres.SIMPLE);

        assertEquals(0, new BigDecimal("1200000.00").compareTo(plan.montoTotal()));
        assertEquals(0, repositorios.prestamos().contar());
    }

    @Test
    @DisplayName("No deja marcar como pagado un prestamo que aun debe")
    void noMarcaPagadoConSaldo() {
        Prestamo prestamo = servicio.crear(cliente.getId(), empleado.getId(),
                new BigDecimal("1000000"), new BigDecimal("2"), 10, TipoInteres.SIMPLE);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.cambiarEstado(prestamo.getId(), EstadoPrestamo.PAGADO));

        assertTrue(error.getMessage().contains("aun debe"));
    }
}
