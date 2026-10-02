package com.crediya.repositorio.jdbc;

import com.crediya.modelo.Empleado;
import com.crediya.modelo.Rol;
import com.crediya.repositorio.EmpleadoRepositorio;

import java.util.Optional;

/**
 * Empleados almacenados en la tabla {@code empleados}.
 */
public class JdbcEmpleadoRepositorio extends RepositorioJdbc<Empleado> implements EmpleadoRepositorio {

    private static final String TABLA = "empleados";

    private static final String SQL_INSERTAR =
            "INSERT INTO empleados (nombre, documento, rol, correo, salario) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE empleados SET nombre = ?, documento = ?, rol = ?, correo = ?, salario = ? WHERE id = ?";

    @Override
    protected String tabla() {
        return TABLA;
    }

    @Override
    protected MapeadorFila<Empleado> mapeador() {
        return fila -> new Empleado(
                fila.getInt("id"),
                fila.getString("nombre"),
                fila.getString("documento"),
                Rol.desde(fila.getString("rol")),
                fila.getString("correo"),
                fila.getBigDecimal("salario"));
    }

    @Override
    public Empleado guardar(Empleado empleado) {
        if (empleado.esNueva()) {
            int id = insertar(SQL_INSERTAR,
                    empleado.getNombre(),
                    empleado.getDocumento(),
                    empleado.getRol().name(),
                    empleado.getCorreo(),
                    empleado.getSalario());
            empleado.setId(id);
        } else {
            ejecutar(SQL_ACTUALIZAR,
                    empleado.getNombre(),
                    empleado.getDocumento(),
                    empleado.getRol().name(),
                    empleado.getCorreo(),
                    empleado.getSalario(),
                    empleado.getId());
        }
        return empleado;
    }

    @Override
    public Optional<Empleado> buscarPorDocumento(String documento) {
        return consultar("SELECT * FROM empleados WHERE documento = ?", documento).stream().findFirst();
    }
}
