package com.crediya.repositorio.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convierte la fila actual de un {@link ResultSet} en un objeto del dominio.
 *
 * <p>Es una interfaz funcional para poder escribir el mapeo como una lambda o
 * una referencia a metodo y dejar que {@link RepositorioJdbc} se encargue del
 * ritual de abrir, recorrer y cerrar. Declara {@code throws SQLException} a
 * proposito: asi el mapeo se escribe sin try-catch y la traduccion del error
 * ocurre en un solo sitio.</p>
 *
 * @param <T> tipo de objeto producido
 */
@FunctionalInterface
public interface MapeadorFila<T> {

    T mapear(ResultSet fila) throws SQLException;
}
