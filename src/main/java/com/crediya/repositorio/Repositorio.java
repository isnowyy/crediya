package com.crediya.repositorio;

import com.crediya.modelo.Identificable;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones que cualquier almacen debe ofrecer sobre una entidad (patron
 * Repositorio).
 *
 * <p>Esta interfaz es la frontera entre el dominio y la tecnologia. Los
 * servicios dependen de ella y no de JDBC ni de archivos, de modo que cambiar
 * de almacen no obliga a tocar una sola regla de negocio: es la inversion de
 * dependencias (la D de SOLID) y es lo que permite probar los servicios con
 * repositorios en memoria.</p>
 *
 * @param <T> tipo de entidad administrada
 */
public interface Repositorio<T extends Identificable> {

    /**
     * Inserta la entidad si es nueva o actualiza la existente.
     *
     * @return la misma entidad, ya con su identificador asignado
     */
    T guardar(T entidad);

    Optional<T> buscarPorId(int id);

    List<T> listar();

    /**
     * @return {@code true} si habia algo que borrar
     */
    boolean eliminar(int id);

    default long contar() {
        return listar().size();
    }
}
