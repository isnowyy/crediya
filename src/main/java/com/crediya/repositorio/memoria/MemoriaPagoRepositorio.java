package com.crediya.repositorio.memoria;

import com.crediya.modelo.Pago;
import com.crediya.repositorio.PagoRepositorio;

import java.util.Comparator;
import java.util.List;

/** Pagos en memoria. */
public class MemoriaPagoRepositorio extends RepositorioMemoria<Pago> implements PagoRepositorio {

    @Override
    public List<Pago> buscarPorPrestamo(int prestamoId) {
        return listar().stream()
                .filter(pago -> pago.getPrestamoId() == prestamoId)
                .sorted(Comparator.comparing(Pago::getFechaPago))
                .toList();
    }
}
