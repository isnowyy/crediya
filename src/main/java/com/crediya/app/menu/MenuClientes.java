package com.crediya.app.menu;

import com.crediya.app.ContextoAplicacion;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Prestamo;
import com.crediya.servicio.ClienteServicio;
import com.crediya.util.Dinero;

import java.util.List;

/**
 * Modulo de clientes: registro, consulta y cartera de cada uno.
 */
public class MenuClientes extends Menu {

    private final ClienteServicio servicio;

    public MenuClientes(ContextoAplicacion contexto) {
        super(contexto.getConsola());
        this.servicio = contexto.getClienteServicio();
    }

    @Override
    protected String titulo() {
        return "Modulo de clientes";
    }

    @Override
    protected List<OpcionMenu> opciones() {
        return List.of(
                OpcionMenu.de("Registrar cliente", this::registrar),
                OpcionMenu.de("Listar clientes", this::listar),
                OpcionMenu.de("Buscar por documento", this::buscarPorDocumento),
                OpcionMenu.de("Buscar por nombre", this::buscarPorNombre),
                OpcionMenu.de("Consultar prestamos de un cliente", this::verPrestamos));
    }

    private void registrar() {
        consola.subtitulo("Nuevo cliente");

        String nombre = consola.leerTexto("  Nombre completo: ");
        String documento = consola.leerTexto("  Documento: ");
        String correo = consola.leerTexto("  Correo: ");
        String telefono = consola.leerTexto("  Telefono: ");

        Cliente cliente = servicio.registrar(nombre, documento, correo, telefono);
        consola.exito("Cliente registrado con el identificador #" + cliente.getId());
    }

    private void listar() {
        consola.subtitulo("Clientes registrados");
        consola.listar(servicio.listar(), Cliente::resumen, "Aun no hay clientes registrados.");
    }

    private void buscarPorDocumento() {
        consola.subtitulo("Buscar cliente");
        String documento = consola.leerTexto("  Documento: ");

        Cliente cliente = servicio.buscarPorDocumento(documento);
        consola.saltoDeLinea();
        consola.mostrar("  %s", cliente.resumen());
        consola.mostrar("  Correo: %s", cliente.getCorreo());
    }

    private void buscarPorNombre() {
        consola.subtitulo("Buscar cliente por nombre");
        String fragmento = consola.leerTexto("  Parte del nombre: ");

        consola.listar(servicio.buscarPorNombre(fragmento), Cliente::resumen,
                "Ningun cliente coincide con esa busqueda.");
    }

    private void verPrestamos() {
        consola.subtitulo("Cartera de un cliente");
        listar();

        int id = consola.leerEntero("  Identificador del cliente: ");
        Cliente cliente = servicio.buscarPorId(id);
        List<Prestamo> prestamos = servicio.prestamosDe(id);

        consola.saltoDeLinea();
        consola.mostrar("  Cliente: %s", cliente.resumen());
        consola.listar(prestamos, Prestamo::resumen, "Este cliente no tiene prestamos.");

        if (!prestamos.isEmpty()) {
            consola.mostrar("  Deuda total: %s", Dinero.formatear(prestamos.stream()
                    .map(Prestamo::getSaldoPendiente)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)));
        }
    }
}
