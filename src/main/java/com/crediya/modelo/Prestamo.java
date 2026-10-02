package com.crediya.modelo;

import com.crediya.excepcion.ReglaNegocioException;
import com.crediya.excepcion.ValidacionException;
import com.crediya.modelo.interes.CalculadoraInteres;
import com.crediya.modelo.interes.CalculadoraInteresFactory;
import com.crediya.modelo.interes.PlanPago;
import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Credito otorgado a un cliente por un empleado.
 *
 * <p>Es el nucleo del dominio y tambien la raiz de su propio agregado: el
 * prestamo es el dueno de sus pagos, asi que el saldo, la mora y el cambio a
 * estado PAGADO se deciden aqui y no en un servicio. Cualquier capa que quiera
 * abonar tiene que pasar por {@link #registrarPago(Pago)}, que es lo que
 * garantiza que nunca se abone mas de lo que se debe.</p>
 *
 * <p>Se construye con {@link Builder} porque son nueve datos y varios
 * opcionales; un constructor con nueve parametros del mismo tipo se presta para
 * invertir argumentos sin que el compilador avise.</p>
 */
public class Prestamo implements Identificable {

    private Integer id;
    private final Cliente cliente;
    private final Empleado empleado;
    private final BigDecimal monto;
    private final BigDecimal tasaInteresMensual;
    private final int cuotas;
    private final LocalDate fechaInicio;
    private final TipoInteres tipoInteres;

    /** Capital mas intereses. Se deriva de los datos anteriores, no se recibe. */
    private final BigDecimal montoTotal;

    /** Valor de cada cuota mensual. Tambien derivado. */
    private final BigDecimal valorCuota;

    private EstadoPrestamo estado;
    private final List<Pago> pagos = new ArrayList<>();

    private Prestamo(Builder builder) {
        this.id = builder.id;
        this.cliente = Validaciones.noNulo(builder.cliente, "cliente");
        this.empleado = Validaciones.noNulo(builder.empleado, "empleado");
        this.monto = Validaciones.montoPositivo(builder.monto, "monto");
        this.tasaInteresMensual = Validaciones.porcentaje(builder.tasaInteresMensual, "interes");
        this.cuotas = Validaciones.enteroEnRango(builder.cuotas, 1, 120, "cuotas");
        this.fechaInicio = Validaciones.noNulo(builder.fechaInicio, "fecha de inicio");
        this.tipoInteres = Validaciones.noNulo(builder.tipoInteres, "tipo de interes");
        this.estado = Validaciones.noNulo(builder.estado, "estado");

        if (this.cliente.getId() == null) {
            throw new ValidacionException("El cliente debe estar registrado antes de crear el prestamo.");
        }
        if (this.empleado.getId() == null) {
            throw new ValidacionException("El empleado debe estar registrado antes de crear el prestamo.");
        }

        CalculadoraInteres calculadora = CalculadoraInteresFactory.para(this.tipoInteres);
        PlanPago plan = calculadora.calcular(this.monto, this.tasaInteresMensual, this.cuotas);
        this.montoTotal = plan.montoTotal();
        this.valorCuota = plan.valorCuota();
    }

    // ------------------------------------------------------------------
    // Reglas de negocio
    // ------------------------------------------------------------------

    /** Suma de todos los abonos recibidos. */
    public BigDecimal getTotalAbonado() {
        return Dinero.normalizar(pagos.stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Lo que todavia debe el cliente. Nunca es negativo. */
    public BigDecimal getSaldoPendiente() {
        BigDecimal saldo = montoTotal.subtract(getTotalAbonado());
        return saldo.signum() < 0 ? Dinero.cero() : Dinero.normalizar(saldo);
    }

    /** Porcentaje de la deuda ya cubierto, entre 0 y 100. */
    public BigDecimal getPorcentajePagado() {
        if (montoTotal.signum() == 0) {
            return Dinero.cero();
        }
        return Dinero.normalizar(getTotalAbonado()
                .multiply(Dinero.CIEN)
                .divide(montoTotal, Dinero.CALCULO));
    }

    /** Fecha en la que vence la ultima cuota. */
    public LocalDate getFechaVencimiento() {
        return fechaInicio.plusMonths(cuotas);
    }

    /**
     * Un prestamo esta vencido cuando ya paso su fecha de vencimiento y aun
     * queda saldo por cobrar.
     */
    public boolean estaVencido(LocalDate referencia) {
        return estado == EstadoPrestamo.PENDIENTE
                && getSaldoPendiente().signum() > 0
                && referencia.isAfter(getFechaVencimiento());
    }

    /** Dias transcurridos desde el vencimiento. Cero si aun esta al dia. */
    public long diasDeMora(LocalDate referencia) {
        if (!estaVencido(referencia)) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(getFechaVencimiento(), referencia);
    }

    /** Un prestamo esta activo mientras no se haya cancelado del todo. */
    public boolean estaActivo() {
        return estado == EstadoPrestamo.PENDIENTE;
    }

    /**
     * Registra un abono y actualiza el estado del prestamo.
     *
     * @throws ReglaNegocioException si el prestamo ya esta pagado, si el pago
     *         pertenece a otro prestamo o si el abono supera el saldo
     */
    public void registrarPago(Pago pago) {
        Validaciones.noNulo(pago, "pago");

        if (estado == EstadoPrestamo.PAGADO) {
            throw new ReglaNegocioException(
                    "El prestamo #" + id + " ya esta pagado; no admite mas abonos.");
        }
        if (id != null && pago.getPrestamoId() != id) {
            throw new ReglaNegocioException(
                    "El pago corresponde al prestamo #" + pago.getPrestamoId()
                            + " y se intento aplicar al #" + id + ".");
        }
        if (pago.getMonto().compareTo(getSaldoPendiente()) > 0) {
            throw new ReglaNegocioException(String.format(
                    "El abono de %s supera el saldo pendiente de %s.",
                    Dinero.formatear(pago.getMonto()), Dinero.formatear(getSaldoPendiente())));
        }

        pagos.add(pago);

        if (getSaldoPendiente().signum() == 0) {
            estado = EstadoPrestamo.PAGADO;
        }
    }

    /**
     * Carga el historico de pagos leido del almacen.
     *
     * <p>A diferencia de {@link #registrarPago(Pago)} no valida reglas de
     * negocio ni cambia el estado: esos pagos ya fueron aceptados en su momento
     * y el estado viene guardado. Solo lo usan los repositorios al reconstruir
     * el objeto.</p>
     */
    public void cargarHistorico(List<Pago> historico) {
        pagos.clear();
        if (historico != null) {
            pagos.addAll(historico);
        }
    }

    /** Linea de una sola fila para los listados de consola. */
    public String resumen() {
        return String.format("#%s | %s | total %s | cuota %s | saldo %s | %s",
                id, cliente.getNombre(), Dinero.formatear(montoTotal),
                Dinero.formatear(valorCuota), Dinero.formatear(getSaldoPendiente()), estado.name());
    }

    // ------------------------------------------------------------------
    // Acceso al estado
    // ------------------------------------------------------------------

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getTasaInteresMensual() {
        return tasaInteresMensual;
    }

    public int getCuotas() {
        return cuotas;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public TipoInteres getTipoInteres() {
        return tipoInteres;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public BigDecimal getValorCuota() {
        return valorCuota;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = Validaciones.noNulo(estado, "estado");
    }

    /** Copia de solo lectura: nadie de afuera puede alterar el historico. */
    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Prestamo prestamo)) {
            return false;
        }
        return id != null && id.equals(prestamo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return resumen();
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construye prestamos paso a paso. Los valores opcionales traen el
     * comportamiento por defecto de un credito nuevo: empieza hoy, con interes
     * simple y en estado pendiente.
     */
    public static class Builder {

        private Integer id;
        private Cliente cliente;
        private Empleado empleado;
        private BigDecimal monto;
        private BigDecimal tasaInteresMensual;
        private int cuotas;
        private LocalDate fechaInicio = LocalDate.now();
        private TipoInteres tipoInteres = TipoInteres.SIMPLE;
        private EstadoPrestamo estado = EstadoPrestamo.PENDIENTE;

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        public Builder cliente(Cliente cliente) {
            this.cliente = cliente;
            return this;
        }

        public Builder empleado(Empleado empleado) {
            this.empleado = empleado;
            return this;
        }

        public Builder monto(BigDecimal monto) {
            this.monto = monto;
            return this;
        }

        public Builder tasaInteresMensual(BigDecimal tasaInteresMensual) {
            this.tasaInteresMensual = tasaInteresMensual;
            return this;
        }

        public Builder cuotas(int cuotas) {
            this.cuotas = cuotas;
            return this;
        }

        public Builder fechaInicio(LocalDate fechaInicio) {
            this.fechaInicio = fechaInicio;
            return this;
        }

        public Builder tipoInteres(TipoInteres tipoInteres) {
            this.tipoInteres = tipoInteres;
            return this;
        }

        public Builder estado(EstadoPrestamo estado) {
            this.estado = estado;
            return this;
        }

        public Prestamo construir() {
            return new Prestamo(this);
        }
    }
}
