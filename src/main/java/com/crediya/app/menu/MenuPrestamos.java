package com.crediya.app.menu;

import com.crediya.app.ContextoAplicacion;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.TipoInteres;
import com.crediya.modelo.interes.PlanPago;
import com.crediya.servicio.ClienteServicio;
import com.crediya.servicio.EmpleadoServicio;
import com.crediya.servicio.PrestamoServicio;
import com.crediya.util.Dinero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo de prestamos: simulacion, otorgamiento y seguimiento.
 */
public class MenuPrestamos extends Menu {

    private final PrestamoServicio servicio;
    private final ClienteServicio clienteServicio;
    private final EmpleadoServicio empleadoServicio;

    public MenuPrestamos(ContextoAplicacion contexto) {
        super(contexto.getConsola());
        this.servicio = contexto.getPrestamoServicio();
        this.clienteServicio = contexto.getClienteServicio();
        this.empleadoServicio = contexto.getEmpleadoServicio();
    }

    @Override
    protected String titulo() {
        return "Modulo de prestamos";
    }

    @Override
    protected List<OpcionMenu> opciones() {
        return List.of(
                OpcionMenu.de("Simular un credito (no se guarda)", this::simular),
                OpcionMenu.de("Otorgar prestamo", this::crear),
                OpcionMenu.de("Listar prestamos", this::listar),
                OpcionMenu.de("Ver detalle de un prestamo", this::verDetalle),
                OpcionMenu.de("Cambiar estado", this::cambiarEstado),
                OpcionMenu.de("Prestamos colocados por un empleado", this::porEmpleado));
    }

    private void simular() {
        consola.subtitulo("Simulacion de credito");

        BigDecimal monto = consola.leerDecimal("  Monto a prestar: ");
        BigDecimal interes = consola.leerDecimal("  Interes mensual (%): ");
        int cuotas = consola.leerEntero("  Numero de cuotas: ");
        TipoInteres tipo = consola.leerOpcionEnum("  Modalidad: ", TipoInteres.class,
                TipoInteres::getDescripcion);

        PlanPago plan = servicio.simular(monto, interes, cuotas, tipo);

        consola.saltoDeLinea();
        consola.mostrar("  Capital      : %s", Dinero.formatear(monto));
        consola.mostrar("  Monto total  : %s", Dinero.formatear(plan.montoTotal()));
        consola.mostrar("  Intereses    : %s", Dinero.formatear(plan.montoTotal().subtract(monto)));
        consola.mostrar("  Cuota mensual: %s  x %d meses",
                Dinero.formatear(plan.valorCuota()), cuotas);
        consola.aviso("Simulacion informativa: no quedo nada registrado.");
    }

    private void crear() {
        consola.subtitulo("Otorgar prestamo");

        consola.mostrar("  Clientes disponibles:");
        consola.listar(clienteServicio.listar(), Cliente::resumen, "No hay clientes registrados.");
        int clienteId = consola.leerEntero("  Identificador del cliente: ");

        consola.mostrar("  Empleados disponibles:");
        consola.listar(empleadoServicio.listar(), Empleado::resumen, "No hay empleados registrados.");
        int empleadoId = consola.leerEntero("  Identificador del empleado: ");

        BigDecimal monto = consola.leerDecimal("  Monto a prestar: ");
        BigDecimal interes = consola.leerDecimal("  Interes mensual (%): ");
        int cuotas = consola.leerEntero("  Numero de cuotas: ");
        TipoInteres tipo = consola.leerOpcionEnum("  Modalidad: ", TipoInteres.class,
                TipoInteres::getDescripcion);

        Prestamo prestamo = servicio.crear(clienteId, empleadoId, monto, interes, cuotas, tipo);

        consola.saltoDeLinea();
        consola.exito("Prestamo #" + prestamo.getId() + " otorgado.");
        imprimirDetalle(prestamo);
    }

    private void listar() {
        consola.subtitulo("Prestamos registrados");
        consola.listar(servicio.listar(), Prestamo::resumen, "Aun no hay prestamos registrados.");
    }

    private void verDetalle() {
        consola.subtitulo("Detalle de prestamo");
        listar();

        int id = consola.leerEntero("  Identificador del prestamo: ");
        imprimirDetalle(servicio.buscarPorId(id));
    }

    private void cambiarEstado() {
        consola.subtitulo("Cambiar estado");
        listar();

        int id = consola.leerEntero("  Identificador del prestamo: ");
        EstadoPrestamo estado = consola.leerOpcionEnum("  Nuevo estado: ", EstadoPrestamo.class,
                EstadoPrestamo::getDescripcion);

        Prestamo prestamo = servicio.cambiarEstado(id, estado);
        consola.exito("El prestamo #" + prestamo.getId() + " quedo en estado " + prestamo.getEstado());
    }

    private void porEmpleado() {
        consola.subtitulo("Prestamos por empleado");
        consola.listar(empleadoServicio.listar(), Empleado::resumen, "No hay empleados registrados.");

        int id = consola.leerEntero("  Identificador del empleado: ");
        consola.listar(servicio.buscarPorEmpleado(id), Prestamo::resumen,
                "Este empleado no ha colocado prestamos.");
    }

    private void imprimirDetalle(Prestamo prestamo) {
        LocalDate hoy = LocalDate.now();

        consola.saltoDeLinea();
        consola.mostrar("  Prestamo #%d", prestamo.getId());
        consola.mostrar("  Cliente       : %s (doc. %s)",
                prestamo.getCliente().getNombre(), prestamo.getCliente().getDocumento());
        consola.mostrar("  Asesor        : %s", prestamo.getEmpleado().getNombre());
        consola.mostrar("  Modalidad     : %s", prestamo.getTipoInteres().getDescripcion());
        consola.mostrar("  Capital       : %s", Dinero.formatear(prestamo.getMonto()));
        consola.mostrar("  Interes       : %s %% mensual",
                prestamo.getTasaInteresMensual().toPlainString());
        consola.mostrar("  Monto total   : %s", Dinero.formatear(prestamo.getMontoTotal()));
        consola.mostrar("  Cuota mensual : %s  x %d cuotas",
                Dinero.formatear(prestamo.getValorCuota()), prestamo.getCuotas());
        consola.mostrar("  Inicio        : %s", prestamo.getFechaInicio());
        consola.mostrar("  Vencimiento   : %s", prestamo.getFechaVencimiento());
        consola.mostrar("  Abonado       : %s (%s %%)",
                Dinero.formatear(prestamo.getTotalAbonado()),
                prestamo.getPorcentajePagado().toPlainString());
        consola.mostrar("  Saldo         : %s", Dinero.formatear(prestamo.getSaldoPendiente()));
        consola.mostrar("  Estado        : %s", prestamo.getEstado().getDescripcion());

        if (prestamo.estaVencido(hoy)) {
            consola.error("VENCIDO hace " + prestamo.diasDeMora(hoy) + " dias.");
        }

        List<Pago> pagos = prestamo.getPagos();
        if (!pagos.isEmpty()) {
            consola.mostrar("  Historico de abonos:");
            pagos.forEach(pago -> consola.mostrar("    %s  %s",
                    pago.getFechaPago(), Dinero.formatear(pago.getMonto())));
        }
    }
}
