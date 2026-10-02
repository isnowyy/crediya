package com.crediya.repositorio.archivo;

import com.crediya.modelo.Empleado;
import com.crediya.modelo.Rol;
import com.crediya.repositorio.EmpleadoRepositorio;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Empleados guardados en {@code datos/empleados.txt}.
 */
public class ArchivoEmpleadoRepositorio extends RepositorioArchivo<Empleado> implements EmpleadoRepositorio {

    public ArchivoEmpleadoRepositorio(Path archivo) {
        super(archivo);
    }

    @Override
    protected String cabecera() {
        return "id;nombre;documento;rol;correo;salario";
    }

    @Override
    protected String serializar(Empleado empleado) {
        return String.join(SEPARADOR,
                String.valueOf(empleado.getId()),
                limpiar(empleado.getNombre()),
                limpiar(empleado.getDocumento()),
                empleado.getRol().name(),
                limpiar(empleado.getCorreo()),
                empleado.getSalario().toPlainString());
    }

    @Override
    protected Empleado deserializar(String[] campos) {
        return new Empleado(
                Integer.valueOf(campos[0].trim()),
                campos[1],
                campos[2],
                Rol.desde(campos[3]),
                campos[4],
                new BigDecimal(campos[5].trim()));
    }

    @Override
    public Optional<Empleado> buscarPorDocumento(String documento) {
        return listar().stream()
                .filter(empleado -> empleado.getDocumento().equals(documento))
                .findFirst();
    }
}
