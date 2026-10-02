package com.crediya.repositorio.jdbc;

import com.crediya.modelo.Pago;
import com.crediya.repositorio.PagoRepositorio;

import java.util.List;

/**
 * Pagos almacenados en la tabla {@code pagos}.
 *
 * <p>Un pago no se modifica nunca: es el registro de un hecho. Por eso
 * {@code guardar} solo contempla la insercion.</p>
 */
public class JdbcPagoRepositorio extends RepositorioJdbc<Pago> implements PagoRepositorio {

    private static final String TABLA = "pagos";

    private static final String SQL_INSERTAR =
            "INSERT INTO pagos (prestamo_id, fecha_pago, monto) VALUES (?, ?, ?)";

    @Override
    protected String tabla() {
        return TABLA;
    }

    @Override
    protected MapeadorFila<Pago> mapeador() {
        return fila -> new Pago(
                fila.getInt("id"),
                fila.getInt("prestamo_id"),
                fila.getDate("fecha_pago").toLocalDate(),
                fila.getBigDecimal("monto"));
    }

    @Override
    public Pago guardar(Pago pago) {
        if (!pago.esNueva()) {
            throw new UnsupportedOperationException(
                    "Un pago ya registrado no se modifica; anule el prestamo si hubo un error.");
        }
        int id = insertar(SQL_INSERTAR, pago.getPrestamoId(), pago.getFechaPago(), pago.getMonto());
        pago.setId(id);
        return pago;
    }

    @Override
    public List<Pago> buscarPorPrestamo(int prestamoId) {
        return consultar("SELECT * FROM pagos WHERE prestamo_id = ? ORDER BY fecha_pago, id", prestamoId);
    }
}
