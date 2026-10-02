package com.crediya.repositorio.memoria;

import com.crediya.modelo.Identificable;
import com.crediya.repositorio.Repositorio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Almacen en memoria, valido solo mientras el programa este abierto.
 *
 * <p>Cumple dos funciones: permite demostrar el sistema en una maquina donde no
 * hay MySQL instalado, y sobre todo permite probar los servicios sin tocar
 * disco ni base de datos. Que exista esta implementacion es la prueba de que la
 * abstraccion {@link Repositorio} esta bien planteada: los servicios no
 * distinguen con cual de las tres esta trabajando.</p>
 *
 * @param <T> tipo de entidad administrada
 */
public class RepositorioMemoria<T extends Identificable> implements Repositorio<T> {

    /** LinkedHashMap para que los listados salgan en el orden de insercion. */
    protected final Map<Integer, T> entidades = new LinkedHashMap<>();

    private final AtomicInteger secuencia = new AtomicInteger(0);

    @Override
    public T guardar(T entidad) {
        if (entidad.esNueva()) {
            entidad.setId(secuencia.incrementAndGet());
        } else {
            secuencia.updateAndGet(actual -> Math.max(actual, entidad.getId()));
        }
        entidades.put(entidad.getId(), entidad);
        return entidad;
    }

    @Override
    public Optional<T> buscarPorId(int id) {
        return Optional.ofNullable(entidades.get(id));
    }

    @Override
    public List<T> listar() {
        return new ArrayList<>(entidades.values());
    }

    @Override
    public boolean eliminar(int id) {
        return entidades.remove(id) != null;
    }
}
