package com.crediya.servicio;

import com.crediya.excepcion.RecursoNoEncontradoException;
import com.crediya.excepcion.ReglaNegocioException;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.TipoInteres;
import com.crediya.modelo.interes.CalculadoraInteresFactory;
import com.crediya.modelo.interes.PlanPago;
import com.crediya.repositorio.PrestamoRepositorio;
import com.crediya.util.Bitacora;
import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas de negocio sobre los prestamos.
 *
 * <p>El calculo del monto total y de la cuota no esta aqui: lo hace el propio
 * {@link Prestamo} apoyandose en su estrategia de interes. El servicio se ocupa
 * de lo que el prestamo no puede saber por si solo, como que el cliente exista
 * o que no tenga cartera vencida.</p>
 */
public class PrestamoServicio {

    private final PrestamoRepositorio repositorio;
    private final ClienteServicio clienteServicio;
    private final EmpleadoServicio empleadoServicio;

    public PrestamoServicio(PrestamoRepositorio repositorio,
                            ClienteServicio clienteServicio,
                            EmpleadoServicio empleadoServicio) {
        this.repositorio = Validaciones.noNulo(repositorio, "repositorio de prestamos");
        this.clienteServicio = Validaciones.noNulo(clienteServicio, "servicio de clientes");
        this.empleadoServicio = Validaciones.noNulo(empleadoServicio, "servicio de empleados");
    }

    /**
     * Crea un prestamo y lo deja guardado.
     *
     * @throws RecursoNoEncontradoException si el cliente o el empleado no existen
     * @throws ReglaNegocioException        si el cliente tiene cartera vencida
     */
    public Prestamo crear(int clienteId, int empleadoId, BigDecimal monto,
                          BigDecimal tasaMensual, int cuotas, TipoInteres tipoInteres) {

        Cliente cliente = clienteServicio.buscarPorId(clienteId);
        Empleado empleado = empleadoServicio.buscarPorId(empleadoId);

        verificarQueNoTengaCarteraVencida(cliente);

        Prestamo prestamo = Prestamo.builder()
                .cliente(cliente)
                .empleado(empleado)
                .monto(monto)
                .tasaInteresMensual(tasaMensual)
                .cuotas(cuotas)
                .tipoInteres(tipoInteres)
                .fechaInicio(LocalDate.now())
                .estado(EstadoPrestamo.PENDIENTE)
                .construir();

        Prestamo guardado = repositorio.guardar(prestamo);
        Bitacora.info(String.format("Prestamo #%d creado para %s por %s",
                guardado.getId(), cliente.getNombre(), Dinero.formatear(guardado.getMonto())));
        return guardado;
    }

    /**
     * Liquida un credito sin guardarlo, para mostrarle al cliente cuanto
     * pagaria antes de decidirse.
     */
    public PlanPago simular(BigDecimal monto, BigDecimal tasaMensual, int cuotas, TipoInteres tipoInteres) {
        BigDecimal capital = Validaciones.montoPositivo(monto, "monto");
        BigDecimal tasa = Validaciones.porcentaje(tasaMensual, "interes");
        int numeroCuotas = Validaciones.enteroEnRango(cuotas, 1, 120, "cuotas");

        return CalculadoraInteresFactory.para(Validaciones.noNulo(tipoInteres, "tipo de interes"))
                .calcular(capital, tasa, numeroCuotas);
    }

    public List<Prestamo> listar() {
        return repositorio.listar();
    }

    public Prestamo buscarPorId(int id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Prestamo", id));
    }

    public List<Prestamo> buscarPorEmpleado(int empleadoId) {
        Empleado empleado = empleadoServicio.buscarPorId(empleadoId);
        return repositorio.buscarPorEmpleado(empleado.getId());
    }

    /**
     * Cambia el estado manualmente.
     *
     * <p>Marcar como PAGADO un prestamo con saldo se rechaza: el estado es
     * consecuencia de los pagos, no al reves. Para cancelarlo hay que abonar el
     * saldo desde el modulo de pagos.</p>
     */
    public Prestamo cambiarEstado(int id, EstadoPrestamo nuevoEstado) {
        Prestamo prestamo = buscarPorId(id);

        if (nuevoEstado == prestamo.getEstado()) {
            throw new ReglaNegocioException("El prestamo #" + id + " ya esta en estado " + nuevoEstado + ".");
        }
        if (nuevoEstado == EstadoPrestamo.PAGADO && prestamo.getSaldoPendiente().signum() > 0) {
            throw new ReglaNegocioException(String.format(
                    "El prestamo #%d no se puede marcar como pagado: aun debe %s. "
                            + "Registre el abono desde el modulo de pagos.",
                    id, Dinero.formatear(prestamo.getSaldoPendiente())));
        }

        prestamo.setEstado(nuevoEstado);
        Bitacora.info("Prestamo #" + id + " cambio a estado " + nuevoEstado);
        return repositorio.guardar(prestamo);
    }

    /**
     * Un cliente con prestamos vencidos no recibe credito nuevo. Es la regla
     * que protege la cartera y la razon de ser del modulo de reportes.
     */
    private void verificarQueNoTengaCarteraVencida(Cliente cliente) {
        LocalDate hoy = LocalDate.now();

        List<Prestamo> vencidos = repositorio.buscarPorCliente(cliente.getId()).stream()
                .filter(prestamo -> prestamo.estaVencido(hoy))
                .sorted(Comparator.comparing(Prestamo::getFechaVencimiento))
                .toList();

        if (!vencidos.isEmpty()) {
            throw new ReglaNegocioException(String.format(
                    "%s tiene %d prestamo(s) vencido(s); el mas antiguo lleva %d dias de mora. "
                            + "No se puede otorgar credito nuevo.",
                    cliente.getNombre(), vencidos.size(), vencidos.get(0).diasDeMora(hoy)));
        }
    }
}
