package com.crediya.app.menu;

import com.crediya.app.ContextoAplicacion;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.Rol;
import com.crediya.servicio.EmpleadoServicio;
import com.crediya.util.Consola;
import com.crediya.util.Dinero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Modulo de empleados: registro y consulta de la planta de personal.
 */
public class MenuEmpleados extends Menu {

    private final EmpleadoServicio servicio;

    public MenuEmpleados(ContextoAplicacion contexto) {
        super(contexto.getConsola());
        this.servicio = contexto.getEmpleadoServicio();
    }

    @Override
    protected String titulo() {
        return "Modulo de empleados";
    }

    @Override
    protected List<OpcionMenu> opciones() {
        return List.of(
                OpcionMenu.de("Registrar empleado", this::registrar),
                OpcionMenu.de("Listar empleados", this::listar),
                OpcionMenu.de("Buscar por documento", this::buscarPorDocumento),
                OpcionMenu.de("Filtrar por rol", this::filtrarPorRol),
                OpcionMenu.de("Actualizar salario", this::actualizarSalario),
                OpcionMenu.de("Ver nomina y distribucion por rol", this::verNomina));
    }

    private void registrar() {
        consola.subtitulo("Nuevo empleado");

        String nombre = consola.leerTexto("  Nombre completo: ");
        String documento = consola.leerTexto("  Documento: ");
        Rol rol = consola.leerOpcionEnum("  Rol: ", Rol.class, Rol::getDescripcion);
        String correo = consola.leerTexto("  Correo: ");
        BigDecimal salario = consola.leerDecimal("  Salario mensual: ");

        Empleado empleado = servicio.registrar(nombre, documento, rol, correo, salario);
        consola.exito("Empleado registrado con el identificador #" + empleado.getId());
    }

    private void listar() {
        consola.subtitulo("Empleados registrados");
        consola.listar(servicio.listar(), Empleado::resumen, "Aun no hay empleados registrados.");
    }

    private void buscarPorDocumento() {
        consola.subtitulo("Buscar empleado");
        String documento = consola.leerTexto("  Documento: ");

        Empleado empleado = servicio.buscarPorDocumento(documento);
        consola.saltoDeLinea();
        consola.mostrar("  %s", empleado.resumen());
        consola.mostrar("  Correo  : %s", empleado.getCorreo());
        consola.mostrar("  Comision: %s %% sobre lo que coloque",
                empleado.getRol().getPorcentajeComision().toPlainString());
    }

    private void filtrarPorRol() {
        Rol rol = consola.leerOpcionEnum("  Rol a consultar: ", Rol.class, Rol::getDescripcion);

        consola.subtitulo("Empleados con rol " + rol.name());
        consola.listar(servicio.listarPorRol(rol), Empleado::resumen,
                "No hay empleados con ese rol.");
    }

    private void actualizarSalario() {
        consola.subtitulo("Actualizar salario");
        listar();

        int id = consola.leerEntero("  Identificador del empleado: ");
        BigDecimal salario = consola.leerDecimal("  Nuevo salario: ");

        Empleado empleado = servicio.actualizarSalario(id, salario);
        consola.exito("Salario de " + empleado.getNombre() + " actualizado a "
                + Dinero.formatear(empleado.getSalario()));
    }

    private void verNomina() {
        consola.subtitulo("Nomina mensual");
        consola.mostrar("  Total a pagar: %s", Dinero.formatear(servicio.nominaMensual()));
        consola.saltoDeLinea();
        consola.mostrar("  Distribucion por rol:");
        servicio.conteoPorRol().forEach((rol, cantidad) ->
                consola.mostrar("    %-22s %d", rol.getDescripcion(), cantidad));
    }
}
