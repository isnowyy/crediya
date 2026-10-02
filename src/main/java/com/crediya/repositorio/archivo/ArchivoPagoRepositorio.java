package com.crediya.repositorio.archivo;

import com.crediya.modelo.Pago;
import com.crediya.repositorio.PagoRepositorio;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Pagos guardados en {@code datos/pagos.txt}.
 */
public class ArchivoPagoRepositorio extends RepositorioArchivo<Pago> implements PagoRepositorio {

    public ArchivoPagoRepositorio(Path archivo) {
        super(archivo);
    }

    @Override
    protected String cabecera() {
        return "id;prestamo_id;fecha_pago;monto";
    }

    @Override
    protected String serializar(Pago pago) {
        return String.join(SEPARADOR,
                String.valueOf(pago.getId()),
                String.valueOf(pago.getPrestamoId()),
                pago.getFechaPago().toString(),
                pago.getMonto().toPlainString());
    }

    @Override
    protected Pago deserializar(String[] campos) {
        return new Pago(
                Integer.valueOf(campos[0].trim()),
                Integer.parseInt(campos[1].trim()),
                LocalDate.parse(campos[2].trim()),
                new BigDecimal(campos[3].trim()));
    }

    @Override
    public List<Pago> buscarPorPrestamo(int prestamoId) {
        return listar().stream()
                .filter(pago -> pago.getPrestamoId() == prestamoId)
                .sorted(Comparator.comparing(Pago::getFechaPago))
                .toList();
    }
}
