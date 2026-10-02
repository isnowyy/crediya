package com.crediya.repositorio.memoria;

import com.crediya.modelo.Cliente;
import com.crediya.repositorio.ClienteRepositorio;

import java.util.Optional;

/** Clientes en memoria. */
public class MemoriaClienteRepositorio extends RepositorioMemoria<Cliente> implements ClienteRepositorio {

    @Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        return listar().stream()
                .filter(cliente -> cliente.getDocumento().equals(documento))
                .findFirst();
    }
}
