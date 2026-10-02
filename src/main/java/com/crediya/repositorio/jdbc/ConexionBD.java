package com.crediya.repositorio.jdbc;

import com.crediya.config.Configuracion;
import com.crediya.excepcion.PersistenciaException;
import com.crediya.util.Bitacora;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto unico de acceso a la base de datos (patron Singleton).
 *
 * <p>Es singleton por una razon concreta y no por costumbre: los datos de
 * conexion se leen una sola vez al arrancar y todos los repositorios deben usar
 * exactamente los mismos. Tener varias instancias abriria la puerta a que un
 * repositorio apunte a otra base.</p>
 *
 * <p>Cada operacion abre y cierra su propia {@link Connection} con
 * try-with-resources. No hay pool porque es una aplicacion de consola de un
 * solo usuario: un pool aqui seria complejidad sin beneficio. Lo que si se
 * garantiza es que ninguna conexion queda abierta.</p>
 */
public final class ConexionBD {

    private final String url;
    private final String usuario;
    private final String clave;

    private ConexionBD() {
        Configuracion configuracion = Configuracion.getInstancia();
        this.url = configuracion.obtenerObligatorio("crediya.db.url");
        this.usuario = configuracion.obtenerObligatorio("crediya.db.usuario");
        this.clave = configuracion.obtener("crediya.db.clave", "");
    }

    private static final class Contenedor {
        private static final ConexionBD INSTANCIA = new ConexionBD();
    }

    public static ConexionBD getInstancia() {
        return Contenedor.INSTANCIA;
    }

    /**
     * Abre una conexion nueva. Quien la pide es responsable de cerrarla, cosa
     * que en este proyecto siempre se hace con try-with-resources.
     *
     * @throws PersistenciaException si la base no responde o rechaza las credenciales
     */
    public Connection abrir() {
        try {
            return DriverManager.getConnection(url, usuario, clave);
        } catch (SQLException e) {
            Bitacora.error("Fallo al conectar con " + url, e);
            throw new PersistenciaException(
                    "No se pudo conectar con MySQL en " + url + ". "
                            + "Revise que el servidor este encendido y que las credenciales de "
                            + "crediya.local.properties sean correctas.", e);
        }
    }

    /**
     * Verifica la conexion sin lanzar excepcion. Lo usa el arranque para poder
     * ofrecer una alternativa cuando la base no esta disponible.
     */
    public boolean disponible() {
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            return conexion.isValid(3);
        } catch (SQLException e) {
            Bitacora.advertencia("Base de datos no disponible: " + e.getMessage());
            return false;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }
}
