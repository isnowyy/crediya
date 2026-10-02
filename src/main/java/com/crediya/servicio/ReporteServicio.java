package com.crediya.servicio;

import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.repositorio.EmpleadoRepositorio;
import com.crediya.repositorio.PagoRepositorio;
import com.crediya.repositorio.PrestamoRepositorio;
import com.crediya.servicio.reporte.ClienteMoroso;
import com.crediya.servicio.reporte.ProductividadEmpleado;
import com.crediya.servicio.reporte.ResumenCartera;
import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Consultas analiticas sobre la cartera.
 *
 * <p>Este es el modulo donde se concentran las expresiones lambda y la Stream
 * API. Todos los reportes se arman en memoria a partir de lo que entregan los
 * repositorios, y no con SQL, por una razon de diseno: asi los mismos reportes
 * dan exactamente el mismo resultado trabajando sobre MySQL, sobre archivos o
 * sobre memoria. Un {@code GROUP BY} escrito en SQL solo funcionaria con uno de
 * los tres almacenes.</p>
 *
 * <p>Todas las consultas reciben la fecha de referencia como parametro en su
 * version completa y usan {@code LocalDate.now()} en la version corta. Esto es
 * lo que hace que los reportes se puedan probar de forma automatica: una prueba
 * puede preguntar "que estaria vencido el 1 de enero" sin tener que cambiar el
 * reloj de la maquina.</p>
 */
public class ReporteServicio {

    private final PrestamoRepositorio prestamoRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final EmpleadoRepositorio empleadoRepositorio;

    public ReporteServicio(PrestamoRepositorio prestamoRepositorio,
                           PagoRepositorio pagoRepositorio,
                           EmpleadoRepositorio empleadoRepositorio) {
        this.prestamoRepositorio = Validaciones.noNulo(prestamoRepositorio, "repositorio de prestamos");
        this.pagoRepositorio = Validaciones.noNulo(pagoRepositorio, "repositorio de pagos");
        this.empleadoRepositorio = Validaciones.noNulo(empleadoRepositorio, "repositorio de empleados");
    }

    // ------------------------------------------------------------------
    // Filtros basicos
    // ------------------------------------------------------------------

    /**
     * Punto de entrada generico: recibe la condicion como lambda.
     *
     * <p>Gracias a este metodo la consola puede pedir cualquier filtro sin que
     * haya que agregar un metodo nuevo al servicio por cada pregunta.</p>
     */
    public List<Prestamo> buscar(Predicate<Prestamo> condicion) {
        Validaciones.noNulo(condicion, "condicion de busqueda");
        return prestamoRepositorio.listar().stream()
                .filter(condicion)
                .sorted(Comparator.comparing(Prestamo::getFechaInicio).reversed())
                .toList();
    }

    /** Prestamos todavia pendientes de pago, del que mas debe al que menos. */
    public List<Prestamo> prestamosActivos() {
        return prestamoRepositorio.listar().stream()
                .filter(Prestamo::estaActivo)
                .sorted(Comparator.comparing(Prestamo::getSaldoPendiente).reversed())
                .toList();
    }

    public List<Prestamo> prestamosVencidos() {
        return prestamosVencidos(LocalDate.now());
    }

    /** Prestamos con fecha de vencimiento pasada y saldo abierto, del mas atrasado al menos. */
    public List<Prestamo> prestamosVencidos(LocalDate referencia) {
        return prestamoRepositorio.listar().stream()
                .filter(prestamo -> prestamo.estaVencido(referencia))
                .sorted(Comparator.comparing(Prestamo::getFechaVencimiento))
                .toList();
    }

    /** Prestamos cuyo capital esta dentro de un rango. */
    public List<Prestamo> prestamosEntre(BigDecimal minimo, BigDecimal maximo) {
        BigDecimal desde = Validaciones.noNulo(minimo, "monto minimo");
        BigDecimal hasta = Validaciones.noNulo(maximo, "monto maximo");

        return buscar(prestamo -> prestamo.getMonto().compareTo(desde) >= 0
                && prestamo.getMonto().compareTo(hasta) <= 0);
    }

    /** Cuantos prestamos hay en cada estado. */
    public Map<EstadoPrestamo, Long> conteoPorEstado() {
        return prestamoRepositorio.listar().stream()
                .collect(Collectors.groupingBy(Prestamo::getEstado, Collectors.counting()));
    }

    // ------------------------------------------------------------------
    // Reportes compuestos
    // ------------------------------------------------------------------

    public List<ClienteMoroso> clientesMorosos() {
        return clientesMorosos(LocalDate.now());
    }

    /**
     * Clientes con cartera vencida, ordenados por deuda de mayor a menor.
     *
     * <p>Agrupa los prestamos vencidos por cliente y de cada grupo saca el
     * numero de prestamos, la deuda acumulada y la mora mas alta.</p>
     */
    public List<ClienteMoroso> clientesMorosos(LocalDate referencia) {
        Map<Cliente, List<Prestamo>> vencidosPorCliente = prestamosVencidos(referencia).stream()
                .collect(Collectors.groupingBy(Prestamo::getCliente));

        return vencidosPorCliente.entrySet().stream()
                .map(entrada -> new ClienteMoroso(
                        entrada.getKey(),
                        entrada.getValue().size(),
                        sumar(entrada.getValue(), Prestamo::getSaldoPendiente),
                        entrada.getValue().stream()
                                .mapToLong(prestamo -> prestamo.diasDeMora(referencia))
                                .max()
                                .orElse(0L)))
                .sorted(Comparator.comparing(ClienteMoroso::saldoTotal).reversed())
                .toList();
    }

    /**
     * Colocacion y comision de cada empleado.
     *
     * <p>Parte de la lista completa de empleados y no de la de prestamos, para
     * que tambien aparezcan con cero los que todavia no han colocado nada: un
     * reporte de productividad que esconde a los improductivos no sirve.</p>
     */
    public List<ProductividadEmpleado> productividadPorEmpleado() {
        Map<Integer, List<Prestamo>> prestamosPorEmpleado = prestamoRepositorio.listar().stream()
                .collect(Collectors.groupingBy(prestamo -> prestamo.getEmpleado().getId()));

        return empleadoRepositorio.listar().stream()
                .map(empleado -> {
                    List<Prestamo> suyos = prestamosPorEmpleado.getOrDefault(empleado.getId(), List.of());
                    BigDecimal colocado = sumar(suyos, Prestamo::getMonto);
                    return new ProductividadEmpleado(
                            empleado, suyos.size(), colocado, empleado.comisionPor(colocado));
                })
                .sorted(Comparator.comparing(ProductividadEmpleado::montoColocado).reversed())
                .toList();
    }

    public ResumenCartera resumenCartera() {
        return resumenCartera(LocalDate.now());
    }

    public ResumenCartera resumenCartera(LocalDate referencia) {
        List<Prestamo> prestamos = prestamoRepositorio.listar();

        long activos = prestamos.stream().filter(Prestamo::estaActivo).count();
        long pagados = prestamos.stream()
                .filter(prestamo -> prestamo.getEstado() == EstadoPrestamo.PAGADO)
                .count();
        long vencidos = prestamos.stream()
                .filter(prestamo -> prestamo.estaVencido(referencia))
                .count();

        BigDecimal recaudado = Dinero.normalizar(pagoRepositorio.listar().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return new ResumenCartera(
                prestamos.size(),
                activos,
                pagados,
                vencidos,
                sumar(prestamos, Prestamo::getMonto),
                sumar(prestamos, Prestamo::getSaldoPendiente),
                recaudado);
    }

    /** Los clientes a los que mas se les debe cobrar. */
    public List<ClienteMoroso> topDeudores(int limite) {
        int tope = Validaciones.enteroEnRango(limite, 1, 100, "limite");
        LocalDate hoy = LocalDate.now();

        Map<Cliente, List<Prestamo>> porCliente = prestamoRepositorio.listar().stream()
                .filter(Prestamo::estaActivo)
                .collect(Collectors.groupingBy(Prestamo::getCliente));

        return porCliente.entrySet().stream()
                .map(entrada -> new ClienteMoroso(
                        entrada.getKey(),
                        entrada.getValue().stream().filter(prestamo -> prestamo.estaVencido(hoy)).count(),
                        sumar(entrada.getValue(), Prestamo::getSaldoPendiente),
                        entrada.getValue().stream()
                                .mapToLong(prestamo -> prestamo.diasDeMora(hoy))
                                .max()
                                .orElse(0L)))
                .sorted(Comparator.comparing(ClienteMoroso::saldoTotal).reversed())
                .limit(tope)
                .toList();
    }

    /** Suma monetaria de un campo de una lista de prestamos. */
    private static BigDecimal sumar(List<Prestamo> prestamos,
                                    java.util.function.Function<Prestamo, BigDecimal> campo) {
        return Dinero.normalizar(prestamos.stream()
                .map(campo)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Lista de empleados, util para los reportes que la consola arma a mano. */
    public List<Empleado> empleados() {
        return empleadoRepositorio.listar();
    }
}
