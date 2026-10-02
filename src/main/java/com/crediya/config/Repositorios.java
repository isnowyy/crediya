package com.crediya.config;

import com.crediya.repositorio.ClienteRepositorio;
import com.crediya.repositorio.EmpleadoRepositorio;
import com.crediya.repositorio.PagoRepositorio;
import com.crediya.repositorio.PrestamoRepositorio;

/**
 * Los cuatro repositorios del sistema, ya construidos y apuntando todos al
 * mismo almacen.
 *
 * <p>Se agrupan en un solo objeto para que no sea posible armar una combinacion
 * incoherente, como leer los prestamos de MySQL y los pagos de un archivo.</p>
 *
 * @param tipo      almacen del que provienen
 * @param empleados repositorio de empleados
 * @param clientes  repositorio de clientes
 * @param prestamos repositorio de prestamos
 * @param pagos     repositorio de pagos
 */
public record Repositorios(TipoAlmacen tipo,
                           EmpleadoRepositorio empleados,
                           ClienteRepositorio clientes,
                           PrestamoRepositorio prestamos,
                           PagoRepositorio pagos) {
}
