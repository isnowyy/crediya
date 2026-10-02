package com.crediya.servicio.reporte;

import com.crediya.modelo.Cliente;
import com.crediya.util.Dinero;

import java.math.BigDecimal;

/**
 * Cliente con al menos un prestamo vencido.
 *
 * <p>Es un objeto de transporte: nace del cruce entre clientes y prestamos y
 * existe solo para llevar el resultado del reporte hasta la consola. Es un
 * {@code record} porque no tiene comportamiento propio ni identidad: solo
 * agrupa cifras ya calculadas.</p>
 *
 * @param cliente           deudor
 * @param prestamosVencidos cuantos de sus prestamos pasaron la fecha de vencimiento
 * @param saldoTotal        cuanto suma lo que debe por esos prestamos
 * @param diasMaximoMora    dias de mora del prestamo mas atrasado
 */
public record ClienteMoroso(Cliente cliente,
                            long prestamosVencidos,
                            BigDecimal saldoTotal,
                            long diasMaximoMora) {

    /** Fila lista para imprimir en consola. */
    public String fila() {
        return String.format("%-28s doc. %-12s %d vencido(s)  deuda %-18s mora %d dias",
                cliente.getNombre(), cliente.getDocumento(), prestamosVencidos,
                Dinero.formatear(saldoTotal), diasMaximoMora);
    }
}
