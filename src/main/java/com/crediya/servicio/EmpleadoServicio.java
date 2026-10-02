package com.crediya.servicio;

import com.crediya.excepcion.RecursoNoEncontradoException;
import com.crediya.excepcion.ReglaNegocioException;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.Rol;
import com.crediya.repositorio.EmpleadoRepositorio;
import com.crediya.util.Bitacora;
import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reglas de negocio sobre los empleados.
 *
 * <p>El servicio no sabe si detras hay MySQL o un archivo: recibe un
 * {@link EmpleadoRepositorio} por constructor y trabaja contra esa interfaz.
 * Esa inyeccion es lo que permite probarlo con un repositorio en memoria.</p>
 */
public class EmpleadoServicio {

    private final EmpleadoRepositorio repositorio;

    public EmpleadoServicio(EmpleadoRepositorio repositorio) {
        this.repositorio = Validaciones.noNulo(repositorio, "repositorio de empleados");
    }

    /**
     * Registra un empleado nuevo.
     *
     * @throws ReglaNegocioException si ya existe otro con el mismo documento
     */
    public Empleado registrar(String nombre, String documento, Rol rol, String correo, BigDecimal salario) {
        Empleado empleado = new Empleado(nombre, documento, rol, correo, salario);

        repositorio.buscarPorDocumento(empleado.getDocumento()).ifPresent(existente -> {
            throw new ReglaNegocioException(
                    "Ya hay un empleado con el documento " + existente.getDocumento()
                            + ": " + existente.getNombre() + ".");
        });

        Empleado guardado = repositorio.guardar(empleado);
        Bitacora.info("Empleado registrado: #" + guardado.getId() + " " + guardado.getNombre());
        return guardado;
    }

    public List<Empleado> listar() {
        return repositorio.listar();
    }

    public Empleado buscarPorId(int id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Empleado", id));
    }

    public Empleado buscarPorDocumento(String documento) {
        String limpio = Validaciones.documento(documento);
        return repositorio.buscarPorDocumento(limpio)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Empleado con documento", limpio));
    }

    /** Empleados de un rol, ordenados por nombre. */
    public List<Empleado> listarPorRol(Rol rol) {
        return repositorio.listar().stream()
                .filter(empleado -> empleado.getRol() == rol)
                .sorted(Comparator.comparing(Empleado::getNombre))
                .toList();
    }

    /** Cuantos empleados hay en cada rol. */
    public Map<Rol, Long> conteoPorRol() {
        return repositorio.listar().stream()
                .collect(Collectors.groupingBy(Empleado::getRol, Collectors.counting()));
    }

    /** Suma de los salarios de toda la planta. */
    public BigDecimal nominaMensual() {
        return Dinero.normalizar(repositorio.listar().stream()
                .map(Empleado::getSalario)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public Empleado actualizarSalario(int id, BigDecimal nuevoSalario) {
        Empleado empleado = buscarPorId(id);
        empleado.setSalario(nuevoSalario);
        Bitacora.info("Salario actualizado para el empleado #" + id);
        return repositorio.guardar(empleado);
    }
}
