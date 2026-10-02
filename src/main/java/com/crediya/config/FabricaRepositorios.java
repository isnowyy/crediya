package com.crediya.config;

import com.crediya.repositorio.ClienteRepositorio;
import com.crediya.repositorio.EmpleadoRepositorio;
import com.crediya.repositorio.PagoRepositorio;
import com.crediya.repositorio.archivo.ArchivoClienteRepositorio;
import com.crediya.repositorio.archivo.ArchivoEmpleadoRepositorio;
import com.crediya.repositorio.archivo.ArchivoPagoRepositorio;
import com.crediya.repositorio.archivo.ArchivoPrestamoRepositorio;
import com.crediya.repositorio.jdbc.JdbcClienteRepositorio;
import com.crediya.repositorio.jdbc.JdbcEmpleadoRepositorio;
import com.crediya.repositorio.jdbc.JdbcPagoRepositorio;
import com.crediya.repositorio.jdbc.JdbcPrestamoRepositorio;
import com.crediya.repositorio.memoria.MemoriaClienteRepositorio;
import com.crediya.repositorio.memoria.MemoriaEmpleadoRepositorio;
import com.crediya.repositorio.memoria.MemoriaPagoRepositorio;
import com.crediya.repositorio.memoria.MemoriaPrestamoRepositorio;
import com.crediya.util.Bitacora;

import java.nio.file.Path;

/**
 * Construye el juego de repositorios que corresponde al almacen elegido
 * (patron Factory).
 *
 * <p>Es el unico lugar de todo el proyecto donde se nombran las clases
 * concretas {@code Jdbc...}, {@code Archivo...} y {@code Memoria...}. Gracias a
 * eso, cambiar de MySQL a archivos es cambiar una linea de configuracion y
 * ningun servicio se entera.</p>
 */
public final class FabricaRepositorios {

    private FabricaRepositorios() {
        // Fabrica estatica: no se instancia.
    }

    public static Repositorios crear(TipoAlmacen tipo) {
        Bitacora.info("Construyendo repositorios para el almacen " + tipo);

        return switch (tipo) {
            case MYSQL -> crearMysql();
            case ARCHIVO -> crearArchivo(Configuracion.getInstancia().carpetaDatos());
            case MEMORIA -> crearMemoria();
        };
    }

    /** Atajo que respeta lo indicado en la configuracion. */
    public static Repositorios crearPorDefecto() {
        return crear(Configuracion.getInstancia().almacen());
    }

    private static Repositorios crearMysql() {
        return new Repositorios(
                TipoAlmacen.MYSQL,
                new JdbcEmpleadoRepositorio(),
                new JdbcClienteRepositorio(),
                new JdbcPrestamoRepositorio(),
                new JdbcPagoRepositorio());
    }

    /**
     * Los archivos comparten carpeta y el repositorio de prestamos recibe a los
     * otros tres para poder reconstruir las relaciones.
     */
    public static Repositorios crearArchivo(Path carpeta) {
        EmpleadoRepositorio empleados = new ArchivoEmpleadoRepositorio(carpeta.resolve("empleados.txt"));
        ClienteRepositorio clientes = new ArchivoClienteRepositorio(carpeta.resolve("clientes.txt"));
        PagoRepositorio pagos = new ArchivoPagoRepositorio(carpeta.resolve("pagos.txt"));

        return new Repositorios(
                TipoAlmacen.ARCHIVO,
                empleados,
                clientes,
                new ArchivoPrestamoRepositorio(carpeta.resolve("prestamos.txt"), clientes, empleados, pagos),
                pagos);
    }

    public static Repositorios crearMemoria() {
        return new Repositorios(
                TipoAlmacen.MEMORIA,
                new MemoriaEmpleadoRepositorio(),
                new MemoriaClienteRepositorio(),
                new MemoriaPrestamoRepositorio(),
                new MemoriaPagoRepositorio());
    }
}
