package com.crediya.app.menu;

import com.crediya.app.ContextoAplicacion;
import com.crediya.modelo.Prestamo;
import com.crediya.servicio.ExportadorTexto;
import com.crediya.servicio.ReporteServicio;
import com.crediya.servicio.reporte.ClienteMoroso;
import com.crediya.servicio.reporte.ProductividadEmpleado;
import com.crediya.util.Dinero;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo de reportes: las consultas que le interesan a la gerencia.
 *
 * <p>Cada opcion es una lambda que termina llamando a un metodo de
 * {@link ReporteServicio}, donde vive el filtrado con Stream API.</p>
 */
public class MenuReportes extends Menu {

    private final ReporteServicio servicio;
    private final ExportadorTexto exportador;
    private final Path carpetaDatos;

    public MenuReportes(ContextoAplicacion contexto, Path carpetaDatos) {
        super(contexto.getConsola());
        this.servicio = contexto.getReporteServicio();
        this.exportador = contexto.getExportadorTexto();
        this.carpetaDatos = carpetaDatos;
    }

    @Override
    protected String titulo() {
        return "Modulo de reportes";
    }

    @Override
    protected List<OpcionMenu> opciones() {
        return List.of(
                OpcionMenu.de("Resumen general de la cartera", this::resumen),
                OpcionMenu.de("Prestamos activos", this::activos),
                OpcionMenu.de("Prestamos vencidos", this::vencidos),
                OpcionMenu.de("Clientes morosos", this::morosos),
                OpcionMenu.de("Mayores deudores", this::topDeudores),
                OpcionMenu.de("Productividad y comisiones por empleado", this::productividad),
                OpcionMenu.de("Prestamos por rango de monto", this::porRango),
                OpcionMenu.de("Conteo de prestamos por estado", this::porEstado),
                OpcionMenu.de("Exportar todo a archivos de texto", this::exportar));
    }

    private void resumen() {
        consola.subtitulo("Resumen de cartera al " + LocalDate.now());
        consola.mostrar(servicio.resumenCartera().comoTexto());
    }

    private void activos() {
        consola.subtitulo("Prestamos activos, del mayor saldo al menor");
        consola.listar(servicio.prestamosActivos(), Prestamo::resumen,
                "No hay prestamos activos.");
    }

    private void vencidos() {
        consola.subtitulo("Prestamos vencidos");

        LocalDate hoy = LocalDate.now();
        consola.listar(servicio.prestamosVencidos(hoy),
                prestamo -> String.format("#%-4d %-24s vencio %s  mora %3d dias  saldo %s",
                        prestamo.getId(), prestamo.getCliente().getNombre(),
                        prestamo.getFechaVencimiento(), prestamo.diasDeMora(hoy),
                        Dinero.formatear(prestamo.getSaldoPendiente())),
                "No hay cartera vencida. Buenas noticias.");
    }

    private void morosos() {
        consola.subtitulo("Clientes morosos");
        consola.listar(servicio.clientesMorosos(), ClienteMoroso::fila,
                "Ningun cliente tiene cartera vencida.");
    }

    private void topDeudores() {
        int limite = consola.leerEntero("  Cuantos clientes desea ver: ", 1, 100);

        consola.subtitulo("Los " + limite + " mayores deudores");
        consola.listar(servicio.topDeudores(limite), ClienteMoroso::fila,
                "No hay deuda pendiente.");
    }

    private void productividad() {
        consola.subtitulo("Productividad por empleado");
        consola.listar(servicio.productividadPorEmpleado(), ProductividadEmpleado::fila,
                "No hay empleados registrados.");
    }

    private void porRango() {
        consola.subtitulo("Prestamos por rango de monto");

        BigDecimal minimo = consola.leerDecimal("  Monto minimo: ");
        BigDecimal maximo = consola.leerDecimal("  Monto maximo: ");

        consola.listar(servicio.prestamosEntre(minimo, maximo), Prestamo::resumen,
                "Ningun prestamo cae en ese rango.");
    }

    private void porEstado() {
        consola.subtitulo("Prestamos por estado");
        servicio.conteoPorEstado().forEach((estado, cantidad) ->
                consola.mostrar("  %-12s %d", estado.name(), cantidad));
    }

    private void exportar() {
        consola.subtitulo("Exportar a archivos de texto");

        List<Path> generados = exportador.exportarTodo(carpetaDatos);
        consola.exito("Se generaron " + generados.size() + " archivos:");
        generados.forEach(ruta -> consola.mostrar("    %s", ruta.toAbsolutePath()));
    }
}
