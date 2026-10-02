package com.crediya.repositorio;

import com.crediya.modelo.Prestamo;

import java.util.List;

/**
 * Almacen de prestamos.
 *
 * <p>Las implementaciones devuelven el prestamo con su cliente, su empleado y
 * su historico de pagos ya cargados, para que quien lo reciba pueda preguntarle
 * el saldo sin tener que armar el objeto por partes.</p>
 */
public interface PrestamoRepositorio extends Repositorio<Prestamo> {

    List<Prestamo> buscarPorCliente(int clienteId);

    List<Prestamo> buscarPorEmpleado(int empleadoId);
}
