package com.crediya.app;

import com.crediya.config.Repositorios;
import com.crediya.servicio.ClienteServicio;
import com.crediya.servicio.EmpleadoServicio;
import com.crediya.servicio.ExportadorTexto;
import com.crediya.servicio.PagoServicio;
import com.crediya.servicio.PrestamoServicio;
import com.crediya.servicio.ReporteServicio;
import com.crediya.util.Consola;

/**
 * Arma el sistema completo y lo deja listo para usarse.
 *
 * <p>Es el unico lugar donde se decide quien depende de quien. Ninguna clase
 * crea por su cuenta las que necesita: todas las reciben por constructor y este
 * contexto es el que hace el cableado. Eso es inyeccion de dependencias hecha a
 * mano, sin framework, y es lo que mantiene a los servicios independientes del
 * almacen y probables por separado.</p>
 */
public class ContextoAplicacion {

    private final Repositorios repositorios;
    private final Consola consola;

    private final EmpleadoServicio empleadoServicio;
    private final ClienteServicio clienteServicio;
    private final PrestamoServicio prestamoServicio;
    private final PagoServicio pagoServicio;
    private final ReporteServicio reporteServicio;
    private final ExportadorTexto exportadorTexto;

    public ContextoAplicacion(Repositorios repositorios, Consola consola) {
        this.repositorios = repositorios;
        this.consola = consola;

        this.empleadoServicio = new EmpleadoServicio(repositorios.empleados());
        this.clienteServicio = new ClienteServicio(repositorios.clientes(), repositorios.prestamos());
        this.prestamoServicio = new PrestamoServicio(
                repositorios.prestamos(), clienteServicio, empleadoServicio);
        this.pagoServicio = new PagoServicio(
                repositorios.pagos(), repositorios.prestamos(), prestamoServicio);
        this.reporteServicio = new ReporteServicio(
                repositorios.prestamos(), repositorios.pagos(), repositorios.empleados());
        this.exportadorTexto = new ExportadorTexto(repositorios, reporteServicio);
    }

    public Repositorios getRepositorios() {
        return repositorios;
    }

    public Consola getConsola() {
        return consola;
    }

    public EmpleadoServicio getEmpleadoServicio() {
        return empleadoServicio;
    }

    public ClienteServicio getClienteServicio() {
        return clienteServicio;
    }

    public PrestamoServicio getPrestamoServicio() {
        return prestamoServicio;
    }

    public PagoServicio getPagoServicio() {
        return pagoServicio;
    }

    public ReporteServicio getReporteServicio() {
        return reporteServicio;
    }

    public ExportadorTexto getExportadorTexto() {
        return exportadorTexto;
    }
}
