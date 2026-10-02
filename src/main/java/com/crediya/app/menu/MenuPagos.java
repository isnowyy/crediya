package com.crediya.app.menu;

import com.crediya.app.ContextoAplicacion;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.servicio.PagoServicio;
import com.crediya.servicio.ReporteServicio;
import com.crediya.util.Dinero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo de pagos: abonos a los prestamos y seguimiento del recaudo.
 */
public class MenuPagos extends Menu {

    private final PagoServicio servicio;
    private final ReporteServicio reporteServicio;

    public MenuPagos(ContextoAplicacion contexto) {
        super(contexto.getConsola());
        this.servicio = contexto.getPagoServicio();
        this.reporteServicio = contexto.getReporteServicio();
    }

    @Override
    protected String titulo() {
        return "Modulo de pagos";
    }

    @Override
    protected List<OpcionMenu> opciones() {
        return List.of(
                OpcionMenu.de("Registrar abono", this::registrarAbono),
                OpcionMenu.de("Historico de un prestamo", this::historico),
                OpcionMenu.de("Ultimos pagos recibidos", this::ultimosPagos),
                OpcionMenu.de("Recaudo mes a mes", this::recaudoPorMes));
    }

    private void registrarAbono() {
        consola.subtitulo("Registrar abono");

        List<Prestamo> activos = reporteServicio.prestamosActivos();
        consola.listar(activos, Prestamo::resumen, "No hay prestamos pendientes de pago.");
        if (activos.isEmpty()) {
            return;
        }

        int prestamoId = consola.leerEntero("  Identificador del prestamo: ");
        BigDecimal monto = consola.leerDecimal("  Valor del abono: ");
        LocalDate fecha = consola.leerFecha("  Fecha del pago (aaaa-mm-dd, Enter = hoy): ");

        Pago pago = servicio.registrarAbono(prestamoId, monto, fecha);

        consola.saltoDeLinea();
        consola.exito("Abono #" + pago.getId() + " registrado por " + Dinero.formatear(pago.getMonto()));
        mostrarEstadoDelPrestamo(prestamoId);
    }

    private void historico() {
        consola.subtitulo("Historico de pagos");
        int prestamoId = consola.leerEntero("  Identificador del prestamo: ");

        List<Pago> pagos = servicio.historicoDe(prestamoId);
        consola.listar(pagos,
                pago -> String.format("%s   %s", pago.getFechaPago(), Dinero.formatear(pago.getMonto())),
                "Este prestamo todavia no tiene abonos.");

        mostrarEstadoDelPrestamo(prestamoId);
    }

    private void ultimosPagos() {
        consola.subtitulo("Pagos recibidos, del mas reciente al mas antiguo");
        consola.listar(servicio.listar(), Pago::toString, "Todavia no se ha recibido ningun pago.");
        consola.saltoDeLinea();
        consola.mostrar("  Total recaudado: %s", Dinero.formatear(servicio.totalRecaudado()));
    }

    private void recaudoPorMes() {
        consola.subtitulo("Recaudo mes a mes");

        var recaudo = servicio.recaudoPorMes();
        if (recaudo.isEmpty()) {
            consola.aviso("Todavia no hay pagos registrados.");
            return;
        }
        recaudo.forEach((mes, total) -> consola.mostrar("  %s   %s", mes, Dinero.formatear(total)));
        consola.saltoDeLinea();
        consola.mostrar("  Total acumulado: %s", Dinero.formatear(servicio.totalRecaudado()));
    }

    private void mostrarEstadoDelPrestamo(int prestamoId) {
        Prestamo prestamo = reporteServicio.buscar(p -> p.getId() == prestamoId).stream()
                .findFirst()
                .orElse(null);

        if (prestamo != null) {
            consola.mostrar("  Saldo pendiente: %s  (%s)",
                    Dinero.formatear(prestamo.getSaldoPendiente()), prestamo.getEstado());
        }
    }
}
