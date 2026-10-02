package com.crediya.repositorio;

import com.crediya.modelo.Empleado;

import java.util.Optional;

/**
 * Almacen de empleados.
 *
 * <p>Anade al contrato general la busqueda por documento, que es la llave
 * natural con la que el negocio identifica a una persona.</p>
 */
public interface EmpleadoRepositorio extends Repositorio<Empleado> {

    Optional<Empleado> buscarPorDocumento(String documento);
}
