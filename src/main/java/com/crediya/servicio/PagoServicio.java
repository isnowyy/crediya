package com.crediya.servicio;

import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.repositorio.PagoRepositorio;
import com.crediya.repositorio.PrestamoRepositorio;
import com.crediya.util.Bitacora;
import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Registro de abonos y consulta del historico.
 *
 * <p>Quien decide si un abono es valido es el propio {@link Prestamo}, a traves
 * de {@link Prestamo#registrarPago(Pago)}. Este servicio se limita a orquestar:
 * trae el prestamo, le pasa el abono y persiste el resultado.</p>
 *
 * <p>Alcance conocido: el abono y la actualizacion del prestamo son dos
 * escrituras separadas y no van dentro de una transaccion. Para una aplicacion
 * de consola de un solo usuario el riesgo es despreciable; en un sistema
 * multiusuario habria que envolverlas en una unidad de trabajo. Se deja
 * documentado en lugar de fingir que el problema no existe.</p>
 */
public class PagoServicio {

    private final PagoRepositorio repositorio;
    private final PrestamoRepositorio prestamoRepositorio;
    private final PrestamoServicio prestamoServicio;

    public PagoServicio(PagoRepositorio repositorio,
                        PrestamoRepositorio prestamoRepositorio,
                        PrestamoServicio prestamoServicio) {
        this.repositorio = Validaciones.noNulo(repositorio, "repositorio de pagos");
        this.prestamoRepositorio = Validaciones.noNulo(prestamoRepositorio, "repositorio de prestamos");
        this.prestamoServicio = Validaciones.noNulo(prestamoServicio, "servicio de prestamos");
    }

    /** Abono de hoy. */
    public Pago registrarAbono(int prestamoId, BigDecimal monto) {
        return registrarAbono(prestamoId, monto, LocalDate.now());
    }

    /**
     * Aplica un abono al prestamo y actualiza el saldo.
     *
     * @return el pago ya guardado, con su identificador
     */
    public Pago registrarAbono(int prestamoId, BigDecimal monto, LocalDate fecha) {
        Prestamo prestamo = prestamoServicio.buscarPorId(prestamoId);

        Pago pago = new Pago(null, prestamo.getId(), fecha, monto);
        prestamo.registrarPago(pago);

        Pago guardado = repositorio.guardar(pago);
        prestamoRepositorio.guardar(prestamo);

        Bitacora.info(String.format("Abono de %s aplicado al prestamo #%d; saldo %s",
                Dinero.formatear(guardado.getMonto()), prestamoId,
                Dinero.formatear(prestamo.getSaldoPendiente())));

        if (prestamo.getEstado() == EstadoPrestamo.PAGADO) {
            Bitacora.info("El prestamo #" + prestamoId + " quedo cancelado en su totalidad.");
        }
        return guardado;
    }

    /** Historico de un prestamo, del abono mas antiguo al mas reciente. */
    public List<Pago> historicoDe(int prestamoId) {
        Prestamo prestamo = prestamoServicio.buscarPorId(prestamoId);
        return repositorio.buscarPorPrestamo(prestamo.getId());
    }

    public List<Pago> listar() {
        return repositorio.listar().stream()
                .sorted(Comparator.comparing(Pago::getFechaPago).reversed())
                .toList();
    }

    /** Total recaudado por la empresa. */
    public BigDecimal totalRecaudado() {
        return Dinero.normalizar(repositorio.listar().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Recaudo agrupado por mes, en orden cronologico.
     * {@code TreeMap} porque {@link YearMonth} ya sabe ordenarse solo.
     */
    public Map<YearMonth, BigDecimal> recaudoPorMes() {
        return repositorio.listar().stream()
                .collect(Collectors.groupingBy(
                        pago -> YearMonth.from(pago.getFechaPago()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Pago::getMonto, BigDecimal::add)));
    }
}
