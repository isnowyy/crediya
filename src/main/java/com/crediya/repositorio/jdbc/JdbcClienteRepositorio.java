package com.crediya.repositorio.jdbc;

import com.crediya.modelo.Cliente;
import com.crediya.repositorio.ClienteRepositorio;

import java.util.Optional;

/**
 * Clientes almacenados en la tabla {@code clientes}.
 */
public class JdbcClienteRepositorio extends RepositorioJdbc<Cliente> implements ClienteRepositorio {

    private static final String TABLA = "clientes";

    private static final String SQL_INSERTAR =
            "INSERT INTO clientes (nombre, documento, correo, telefono) VALUES (?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE clientes SET nombre = ?, documento = ?, correo = ?, telefono = ? WHERE id = ?";

    @Override
    protected String tabla() {
        return TABLA;
    }

    @Override
    protected MapeadorFila<Cliente> mapeador() {
        return fila -> new Cliente(
                fila.getInt("id"),
                fila.getString("nombre"),
                fila.getString("documento"),
                fila.getString("correo"),
                fila.getString("telefono"));
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        if (cliente.esNueva()) {
            int id = insertar(SQL_INSERTAR,
                    cliente.getNombre(),
                    cliente.getDocumento(),
                    cliente.getCorreo(),
                    cliente.getTelefono());
            cliente.setId(id);
        } else {
            ejecutar(SQL_ACTUALIZAR,
                    cliente.getNombre(),
                    cliente.getDocumento(),
                    cliente.getCorreo(),
                    cliente.getTelefono(),
                    cliente.getId());
        }
        return cliente;
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        return consultar("SELECT * FROM clientes WHERE documento = ?", documento).stream().findFirst();
    }
}
