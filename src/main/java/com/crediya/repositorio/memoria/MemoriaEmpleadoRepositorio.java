package com.crediya.repositorio.memoria;

import com.crediya.modelo.Empleado;
import com.crediya.repositorio.EmpleadoRepositorio;

import java.util.Optional;

/** Empleados en memoria. */
public class MemoriaEmpleadoRepositorio extends RepositorioMemoria<Empleado> implements EmpleadoRepositorio {

    @Override
    public Optional<Empleado> buscarPorDocumento(String documento) {
        return listar().stream()
                .filter(empleado -> empleado.getDocumento().equals(documento))
                .findFirst();
    }
}
