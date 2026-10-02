package com.crediya.repositorio.jdbc;

import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.Rol;
import com.crediya.modelo.TipoInteres;
import com.crediya.repositorio.PrestamoRepositorio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Prestamos almacenados en la tabla {@code prestamos}.
 *
 * <p>La consulta base trae en un solo viaje el prestamo, su cliente y su
 * empleado mediante {@code JOIN}, y el gancho {@link #completar(List)} reparte
 * los pagos con una segunda consulta para todos los prestamos leidos. Son dos
 * consultas en total sin importar cuantos prestamos haya, en lugar de las
 * {@code 3N + 1} que saldrian de resolver cada relacion por separado.</p>
 */
public class JdbcPrestamoRepositorio extends RepositorioJdbc<Prestamo> implements PrestamoRepositorio {

    private static final String TABLA = "prestamos";

    private static final String SQL_SELECCION = """
            SELECT p.id, p.monto, p.interes, p.cuotas, p.fecha_inicio, p.estado, p.tipo_interes,
                   c.id AS c_id, c.nombre AS c_nombre, c.documento AS c_documento,
                   c.correo AS c_correo, c.telefono AS c_telefono,
                   e.id AS e_id, e.nombre AS e_nombre, e.documento AS e_documento,
                   e.rol AS e_rol, e.correo AS e_correo, e.salario AS e_salario
            FROM        prestamos p
            INNER JOIN  clientes  c ON c.id = p.cliente_id
            INNER JOIN  empleados e ON e.id = p.empleado_id""";

    private static final String SQL_INSERTAR = """
            INSERT INTO prestamos (cliente_id, empleado_id, monto, interes, cuotas,
                                   fecha_inicio, estado, tipo_interes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String SQL_ACTUALIZAR = """
            UPDATE prestamos
               SET cliente_id = ?, empleado_id = ?, monto = ?, interes = ?, cuotas = ?,
                   fecha_inicio = ?, estado = ?, tipo_interes = ?
             WHERE id = ?""";

    @Override
    protected String tabla() {
        return TABLA;
    }

    @Override
    protected String sqlSeleccion() {
        return SQL_SELECCION;
    }

    @Override
    protected String columnaId() {
        return "p.id";
    }

    @Override
    protected MapeadorFila<Prestamo> mapeador() {
        return fila -> {
            Cliente cliente = new Cliente(
                    fila.getInt("c_id"),
                    fila.getString("c_nombre"),
                    fila.getString("c_documento"),
                    fila.getString("c_correo"),
                    fila.getString("c_telefono"));

            Empleado empleado = new Empleado(
                    fila.getInt("e_id"),
                    fila.getString("e_nombre"),
                    fila.getString("e_documento"),
                    Rol.desde(fila.getString("e_rol")),
                    fila.getString("e_correo"),
                    fila.getBigDecimal("e_salario"));

            return Prestamo.builder()
                    .id(fila.getInt("id"))
                    .cliente(cliente)
                    .empleado(empleado)
                    .monto(fila.getBigDecimal("monto"))
                    .tasaInteresMensual(fila.getBigDecimal("interes"))
                    .cuotas(fila.getInt("cuotas"))
                    .fechaInicio(fila.getDate("fecha_inicio").toLocalDate())
                    .estado(EstadoPrestamo.desde(fila.getString("estado")))
                    .tipoInteres(TipoInteres.desde(fila.getString("tipo_interes")))
                    .construir();
        };
    }

    /** Carga el historico de pagos de todos los prestamos leidos en una sola consulta. */
    @Override
    protected List<Prestamo> completar(List<Prestamo> prestamos) {
        if (prestamos.isEmpty()) {
            return prestamos;
        }

        Object[] identificadores = prestamos.stream().map(Prestamo::getId).toArray();
        String marcadores = IntStream.range(0, identificadores.length)
                .mapToObj(indice -> "?")
                .collect(Collectors.joining(", "));

        MapeadorFila<Pago> mapeadorPago = fila -> new Pago(
                fila.getInt("id"),
                fila.getInt("prestamo_id"),
                fila.getDate("fecha_pago").toLocalDate(),
                fila.getBigDecimal("monto"));

        Map<Integer, List<Pago>> pagosPorPrestamo = consultarCon(mapeadorPago,
                "SELECT * FROM pagos WHERE prestamo_id IN (" + marcadores + ") ORDER BY fecha_pago, id",
                identificadores)
                .stream()
                .sorted(Comparator.comparing(Pago::getFechaPago))
                .collect(Collectors.groupingBy(Pago::getPrestamoId));

        prestamos.forEach(prestamo ->
                prestamo.cargarHistorico(pagosPorPrestamo.getOrDefault(prestamo.getId(), List.of())));

        return prestamos;
    }

    @Override
    public Prestamo guardar(Prestamo prestamo) {
        if (prestamo.esNueva()) {
            int id = insertar(SQL_INSERTAR,
                    prestamo.getCliente().getId(),
                    prestamo.getEmpleado().getId(),
                    prestamo.getMonto(),
                    prestamo.getTasaInteresMensual(),
                    prestamo.getCuotas(),
                    prestamo.getFechaInicio(),
                    prestamo.getEstado().name(),
                    prestamo.getTipoInteres().name());
            prestamo.setId(id);
        } else {
            ejecutar(SQL_ACTUALIZAR,
                    prestamo.getCliente().getId(),
                    prestamo.getEmpleado().getId(),
                    prestamo.getMonto(),
                    prestamo.getTasaInteresMensual(),
                    prestamo.getCuotas(),
                    prestamo.getFechaInicio(),
                    prestamo.getEstado().name(),
                    prestamo.getTipoInteres().name(),
                    prestamo.getId());
        }
        return prestamo;
    }

    @Override
    public List<Prestamo> buscarPorCliente(int clienteId) {
        return consultar(SQL_SELECCION + " WHERE p.cliente_id = ? ORDER BY p.id", clienteId);
    }

    @Override
    public List<Prestamo> buscarPorEmpleado(int empleadoId) {
        return consultar(SQL_SELECCION + " WHERE p.empleado_id = ? ORDER BY p.id", empleadoId);
    }
}
