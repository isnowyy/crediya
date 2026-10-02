package com.crediya.repositorio.jdbc;

import com.crediya.excepcion.PersistenciaException;
import com.crediya.modelo.Identificable;
import com.crediya.repositorio.Repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base de los repositorios que hablan con MySQL (patron Template Method).
 *
 * <p>Concentra el ceremonial de JDBC: pedir la conexion, preparar la sentencia,
 * poner los parametros, recorrer el resultado, cerrar todo y traducir
 * {@link SQLException} a {@link PersistenciaException}. Las subclases solo
 * escriben SQL y mapean filas.</p>
 *
 * <p>Todas las sentencias usan {@link PreparedStatement} con parametros: ningun
 * dato del usuario se concatena dentro del SQL, que es la unica forma seria de
 * evitar inyeccion. Los nombres de tabla si se concatenan, pero salen de
 * constantes del propio codigo, nunca de la entrada.</p>
 *
 * @param <T> tipo de entidad administrada
 */
public abstract class RepositorioJdbc<T extends Identificable> implements Repositorio<T> {

    protected final ConexionBD conexionBD = ConexionBD.getInstancia();

    /** Nombre de la tabla. Debe ser una constante del codigo. */
    protected abstract String tabla();

    /** Como convertir una fila en entidad. */
    protected abstract MapeadorFila<T> mapeador();

    /** Consulta base sobre la que se arman {@code listar} y {@code buscarPorId}. */
    protected String sqlSeleccion() {
        return "SELECT * FROM " + tabla();
    }

    /** Columna por la que se identifica una fila; cambia cuando hay alias. */
    protected String columnaId() {
        return tabla() + ".id";
    }

    protected String ordenPorDefecto() {
        return columnaId();
    }

    /**
     * Gancho para completar las entidades despues de leerlas, por ejemplo
     * cargando sus relaciones. Por defecto no hace nada.
     */
    protected List<T> completar(List<T> entidades) {
        return entidades;
    }

    @Override
    public List<T> listar() {
        return consultar(sqlSeleccion() + " ORDER BY " + ordenPorDefecto());
    }

    @Override
    public Optional<T> buscarPorId(int id) {
        return consultar(sqlSeleccion() + " WHERE " + columnaId() + " = ?", id).stream().findFirst();
    }

    @Override
    public boolean eliminar(int id) {
        return ejecutar("DELETE FROM " + tabla() + " WHERE id = ?", id) > 0;
    }

    @Override
    public long contar() {
        try (Connection conexion = conexionBD.abrir();
             PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM " + tabla());
             ResultSet resultado = sentencia.executeQuery()) {

            return resultado.next() ? resultado.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo contar los registros de " + tabla(), e);
        }
    }

    // ------------------------------------------------------------------
    // Apoyo para las subclases
    // ------------------------------------------------------------------

    /** Ejecuta una consulta y devuelve las entidades ya completadas. */
    protected List<T> consultar(String sql, Object... parametros) {
        return completar(consultarCon(mapeador(), sql, parametros));
    }

    /** Ejecuta una consulta con un mapeo distinto al de la entidad principal. */
    protected <R> List<R> consultarCon(MapeadorFila<R> mapeador, String sql, Object... parametros) {
        try (Connection conexion = conexionBD.abrir();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            aplicarParametros(sentencia, parametros);

            try (ResultSet resultado = sentencia.executeQuery()) {
                List<R> filas = new ArrayList<>();
                while (resultado.next()) {
                    filas.add(mapeador.mapear(resultado));
                }
                return filas;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Fallo la consulta sobre " + tabla() + ": " + e.getMessage(), e);
        }
    }

    /** Ejecuta un INSERT, UPDATE o DELETE y devuelve las filas afectadas. */
    protected int ejecutar(String sql, Object... parametros) {
        try (Connection conexion = conexionBD.abrir();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            aplicarParametros(sentencia, parametros);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Fallo la operacion sobre " + tabla() + ": " + e.getMessage(), e);
        }
    }

    /** Ejecuta un INSERT y devuelve el identificador autogenerado. */
    protected int insertar(String sql, Object... parametros) {
        try (Connection conexion = conexionBD.abrir();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            aplicarParametros(sentencia, parametros);
            sentencia.executeUpdate();

            try (ResultSet llaves = sentencia.getGeneratedKeys()) {
                if (llaves.next()) {
                    return llaves.getInt(1);
                }
                throw new PersistenciaException("MySQL no devolvio identificador al insertar en " + tabla());
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo insertar en " + tabla() + ": " + e.getMessage(), e);
        }
    }

    private void aplicarParametros(PreparedStatement sentencia, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            sentencia.setObject(i + 1, parametros[i]);
        }
    }
}
