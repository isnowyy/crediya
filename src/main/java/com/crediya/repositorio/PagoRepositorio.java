package com.crediya.repositorio;

import com.crediya.modelo.Pago;

import java.util.List;

/**
 * Almacen de pagos.
 */
public interface PagoRepositorio extends Repositorio<Pago> {

    /** Historico de abonos de un prestamo, del mas antiguo al mas reciente. */
    List<Pago> buscarPorPrestamo(int prestamoId);
}
