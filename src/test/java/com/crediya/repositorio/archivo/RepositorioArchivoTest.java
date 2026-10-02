package com.crediya.repositorio.archivo;

import com.crediya.config.FabricaRepositorios;
import com.crediya.config.Repositorios;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.Rol;
import com.crediya.modelo.TipoInteres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la persistencia en archivos de texto.
 *
 * <p>Cada prueba trabaja sobre una carpeta temporal que JUnit crea y borra sola
 * ({@link TempDir}), de modo que nunca tocan los archivos reales del proyecto
 * ni dependen de lo que haya dejado la prueba anterior.</p>
 *
 * <p>Lo que se verifica es el viaje de ida y vuelta: lo que se escribe tiene
 * que volver a leerse igual, incluidas las relaciones entre prestamo, cliente,
 * empleado y pagos.</p>
 */
@DisplayName("Repositorios de archivo")
class RepositorioArchivoTest {

    @TempDir
    Path carpeta;

    private Repositorios repositorios;

    @BeforeEach
    void prepararAlmacen() {
        repositorios = FabricaRepositorios.crearArchivo(carpeta);
    }

    @Test
    @DisplayName("Crea el archivo con su cabecera aunque no haya datos")
    void creaArchivoConCabecera() throws IOException {
        Path archivo = carpeta.resolve("empleados.txt");

        assertTrue(Files.exists(archivo));
        assertTrue(Files.readString(archivo).startsWith("# id;nombre;documento;rol;correo;salario"));
    }

    @Test
    @DisplayName("Un empleado guardado se vuelve a leer igual")
    void viajeDeIdaYVueltaDeEmpleado() {
        Empleado guardado = repositorios.empleados().guardar(new Empleado(
                "Ana Gomez", "1098765432", Rol.ASESOR, "ana@crediya.co", new BigDecimal("3200000")));

        assertEquals(1, guardado.getId());

        Empleado leido = repositorios.empleados().buscarPorId(1).orElseThrow();
        assertEquals("Ana Gomez", leido.getNombre());
        assertEquals(Rol.ASESOR, leido.getRol());
        assertEquals(0, new BigDecimal("3200000.00").compareTo(leido.getSalario()));
    }

    @Test
    @DisplayName("Numera cada entidad nueva a partir de la ultima guardada")
    void numeraSecuencialmente() {
        repositorios.clientes().guardar(new Cliente("Pedro Sanchez", "1005432198",
                "pedro@correo.com", "3105558877"));
        Cliente segundo = repositorios.clientes().guardar(new Cliente("Marta Quintero", "63301122",
                "marta@correo.com", "3009991122"));

        assertEquals(2, segundo.getId());
        assertEquals(2, repositorios.clientes().contar());
    }

    @Test
    @DisplayName("Guardar una entidad existente la actualiza en lugar de duplicarla")
    void actualizaSinDuplicar() {
        Empleado empleado = repositorios.empleados().guardar(new Empleado(
                "Ana Gomez", "1098765432", Rol.ASESOR, "ana@crediya.co", new BigDecimal("3200000")));

        empleado.setSalario(new BigDecimal("4000000"));
        repositorios.empleados().guardar(empleado);

        assertEquals(1, repositorios.empleados().contar());
        assertEquals(0, new BigDecimal("4000000.00").compareTo(
                repositorios.empleados().buscarPorId(1).orElseThrow().getSalario()));
    }

    @Test
    @DisplayName("Un prestamo leido trae su cliente, su empleado y sus pagos")
    void reconstruyeLasRelaciones() {
        Cliente cliente = repositorios.clientes().guardar(new Cliente("Pedro Sanchez",
                "1005432198", "pedro@correo.com", "3105558877"));
        Empleado empleado = repositorios.empleados().guardar(new Empleado("Ana Gomez",
                "1098765432", Rol.ASESOR, "ana@crediya.co", new BigDecimal("3200000")));

        Prestamo prestamo = repositorios.prestamos().guardar(Prestamo.builder()
                .cliente(cliente)
                .empleado(empleado)
                .monto(new BigDecimal("1000000"))
                .tasaInteresMensual(new BigDecimal("2"))
                .cuotas(10)
                .fechaInicio(LocalDate.of(2026, 1, 15))
                .tipoInteres(TipoInteres.SIMPLE)
                .construir());

        repositorios.pagos().guardar(new Pago(null, prestamo.getId(),
                LocalDate.of(2026, 2, 15), new BigDecimal("120000")));
        repositorios.pagos().guardar(new Pago(null, prestamo.getId(),
                LocalDate.of(2026, 3, 15), new BigDecimal("120000")));

        // Se construye un almacen nuevo sobre la misma carpeta: nada queda en memoria.
        Repositorios releido = FabricaRepositorios.crearArchivo(carpeta);
        Prestamo recuperado = releido.prestamos().buscarPorId(prestamo.getId()).orElseThrow();

        assertEquals("Pedro Sanchez", recuperado.getCliente().getNombre());
        assertEquals("Ana Gomez", recuperado.getEmpleado().getNombre());
        assertEquals(LocalDate.of(2026, 1, 15), recuperado.getFechaInicio());
        assertEquals(TipoInteres.SIMPLE, recuperado.getTipoInteres());
        assertEquals(2, recuperado.getPagos().size());
        assertEquals(0, new BigDecimal("240000.00").compareTo(recuperado.getTotalAbonado()));
        assertEquals(0, new BigDecimal("960000.00").compareTo(recuperado.getSaldoPendiente()));
    }

    @Test
    @DisplayName("Los pagos de un prestamo salen del mas antiguo al mas reciente")
    void ordenaElHistorico() {
        repositorios.pagos().guardar(new Pago(null, 1, LocalDate.of(2026, 5, 1), new BigDecimal("100")));
        repositorios.pagos().guardar(new Pago(null, 1, LocalDate.of(2026, 2, 1), new BigDecimal("200")));
        repositorios.pagos().guardar(new Pago(null, 2, LocalDate.of(2026, 3, 1), new BigDecimal("300")));

        List<Pago> historico = repositorios.pagos().buscarPorPrestamo(1);

        assertEquals(2, historico.size());
        assertEquals(LocalDate.of(2026, 2, 1), historico.get(0).getFechaPago());
    }

    @Test
    @DisplayName("Eliminar quita la fila del archivo")
    void elimina() {
        repositorios.clientes().guardar(new Cliente("Pedro Sanchez", "1005432198",
                "pedro@correo.com", "3105558877"));

        assertTrue(repositorios.clientes().eliminar(1));
        assertFalse(repositorios.clientes().eliminar(1), "Borrar dos veces debe devolver falso");
        assertEquals(0, repositorios.clientes().contar());
    }

    @Test
    @DisplayName("Un nombre con punto y coma no parte la fila en dos")
    void limpiaElSeparador() {
        repositorios.clientes().guardar(new Cliente("Perez; Juan", "1005432198",
                "juan@correo.com", "3105558877"));

        List<Cliente> clientes = repositorios.clientes().listar();

        assertEquals(1, clientes.size());
        assertEquals("Perez, Juan", clientes.get(0).getNombre());
    }
}
