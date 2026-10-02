package com.crediya.repositorio;

import com.crediya.modelo.Cliente;

import java.util.Optional;

/**
 * Almacen de clientes.
 */
public interface ClienteRepositorio extends Repositorio<Cliente> {

    Optional<Cliente> buscarPorDocumento(String documento);
}
