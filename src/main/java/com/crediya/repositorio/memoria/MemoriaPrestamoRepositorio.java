package com.crediya.repositorio.memoria;

import com.crediya.modelo.Prestamo;
import com.crediya.repositorio.PrestamoRepositorio;

import java.util.List;

/** Prestamos en memoria. */
public class MemoriaPrestamoRepositorio extends RepositorioMemoria<Prestamo> implements PrestamoRepositorio {

    @Override
    public List<Prestamo> buscarPorCliente(int clienteId) {
        return listar().stream()
                .filter(prestamo -> clienteId == prestamo.getCliente().getId())
                .toList();
    }

    @Override
    public List<Prestamo> buscarPorEmpleado(int empleadoId) {
        return listar().stream()
                .filter(prestamo -> empleadoId == prestamo.getEmpleado().getId())
                .toList();
    }
}
