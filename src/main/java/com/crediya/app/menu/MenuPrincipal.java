package com.crediya.app.menu;

import com.crediya.app.ContextoAplicacion;
import com.crediya.app.DatosDemo;
import com.crediya.config.Configuracion;
import com.crediya.util.Dinero;

import java.util.List;

/**
 * Menu raiz del sistema. Reparte hacia los cinco modulos del enunciado.
 */
public class MenuPrincipal extends Menu {

    private final ContextoAplicacion contexto;

    private final MenuEmpleados menuEmpleados;
    private final MenuClientes menuClientes;
    private final MenuPrestamos menuPrestamos;
    private final MenuPagos menuPagos;
    private final MenuReportes menuReportes;

    public MenuPrincipal(ContextoAplicacion contexto) {
        super(contexto.getConsola());
        this.contexto = contexto;

        this.menuEmpleados = new MenuEmpleados(contexto);
        this.menuClientes = new MenuClientes(contexto);
        this.menuPrestamos = new MenuPrestamos(contexto);
        this.menuPagos = new MenuPagos(contexto);
        this.menuReportes = new MenuReportes(contexto, Configuracion.getInstancia().carpetaDatos());
    }

    @Override
    protected String titulo() {
        return "CrediYa S.A.S. - Sistema de cobros de cartera";
    }

    @Override
    protected String etiquetaSalida() {
        return "Salir del sistema";
    }

    @Override
    protected void alEntrar() {
        consola.mostrar("  Almacen activo: %s", contexto.getRepositorios().tipo().getDescripcion());
    }

    @Override
    protected List<OpcionMenu> opciones() {
        return List.of(
                OpcionMenu.de("Empleados", menuEmpleados::ejecutar),
                OpcionMenu.de("Clientes", menuClientes::ejecutar),
                OpcionMenu.de("Prestamos", menuPrestamos::ejecutar),
                OpcionMenu.de("Pagos", menuPagos::ejecutar),
                OpcionMenu.de("Reportes", menuReportes::ejecutar),
                OpcionMenu.de("Cargar datos de demostracion", this::cargarDemo),
                OpcionMenu.de("Estado del sistema", this::estado));
    }

    private void cargarDemo() {
        consola.subtitulo("Datos de demostracion");
        consola.aviso("Se agregaran empleados, clientes, prestamos y pagos de ejemplo "
                + "al almacen activo (" + contexto.getRepositorios().tipo().name() + ").");

        if (!consola.confirmar("  Desea continuar?")) {
            consola.aviso("Operacion cancelada.");
            return;
        }

        int creados = DatosDemo.sembrar(contexto.getRepositorios());
        consola.exito("Se cargaron " + creados + " registros de ejemplo.");
    }

    private void estado() {
        var repositorios = contexto.getRepositorios();

        consola.subtitulo("Estado del sistema");
        consola.mostrar("  Almacen    : %s", repositorios.tipo().getDescripcion());
        consola.mostrar("  Empleados  : %d", repositorios.empleados().contar());
        consola.mostrar("  Clientes   : %d", repositorios.clientes().contar());
        consola.mostrar("  Prestamos  : %d", repositorios.prestamos().contar());
        consola.mostrar("  Pagos      : %d", repositorios.pagos().contar());
        consola.saltoDeLinea();
        consola.mostrar("  Saldo por cobrar: %s",
                Dinero.formatear(contexto.getReporteServicio().resumenCartera().porCobrar()));
    }
}
