package com.crediya.repositorio.archivo;

import com.crediya.modelo.Cliente;
import com.crediya.repositorio.ClienteRepositorio;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Clientes guardados en {@code datos/clientes.txt}.
 */
public class ArchivoClienteRepositorio extends RepositorioArchivo<Cliente> implements ClienteRepositorio {

    public ArchivoClienteRepositorio(Path archivo) {
        super(archivo);
    }

    @Override
    protected String cabecera() {
        return "id;nombre;documento;correo;telefono";
    }

    @Override
    protected String serializar(Cliente cliente) {
        return String.join(SEPARADOR,
                String.valueOf(cliente.getId()),
                limpiar(cliente.getNombre()),
                limpiar(cliente.getDocumento()),
                limpiar(cliente.getCorreo()),
                limpiar(cliente.getTelefono()));
    }

    @Override
    protected Cliente deserializar(String[] campos) {
        return new Cliente(
                Integer.valueOf(campos[0].trim()),
                campos[1],
                campos[2],
                campos[3],
                campos[4]);
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        return listar().stream()
                .filter(cliente -> cliente.getDocumento().equals(documento))
                .findFirst();
    }
}
