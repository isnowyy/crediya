package com.crediya.app;

import com.crediya.app.menu.MenuPrincipal;
import com.crediya.config.Configuracion;
import com.crediya.config.FabricaRepositorios;
import com.crediya.config.Repositorios;
import com.crediya.config.TipoAlmacen;
import com.crediya.excepcion.CrediYaException;
import com.crediya.repositorio.jdbc.ConexionBD;
import com.crediya.util.Bitacora;
import com.crediya.util.Consola;
import com.crediya.util.EntradaCerradaException;

/**
 * Punto de entrada del sistema CrediYa.
 *
 * <p>Su unica responsabilidad es arrancar: elegir el almacen, armar el contexto
 * y entregarle el control al menu principal. Ninguna regla de negocio vive
 * aqui.</p>
 */
public final class Main {

    private Main() {
        // Clase de arranque: no se instancia.
    }

    public static void main(String[] args) {
        try (Consola consola = new Consola()) {
            mostrarBanner(consola);

            Repositorios repositorios = prepararAlmacen(consola);
            if (repositorios == null) {
                consola.aviso("El sistema no puede continuar sin un almacen. Hasta pronto.");
                return;
            }

            ContextoAplicacion contexto = new ContextoAplicacion(repositorios, consola);
            new MenuPrincipal(contexto).ejecutar();

            consola.saltoDeLinea();
            consola.mostrar("  Gracias por usar CrediYa. Hasta pronto.");

        } catch (EntradaCerradaException e) {
            System.out.println();
            System.out.println("  Sesion finalizada.");
        } catch (CrediYaException e) {
            System.err.println("  [!] " + e.getMessage());
            Bitacora.error("El sistema termino por un error no recuperable", e);
            System.exit(1);
        }
    }

    /**
     * Elige el almacen con el que se va a trabajar.
     *
     * <p>Si la configuracion pide MySQL pero la base no responde, no se cae el
     * programa: se explica el problema y se ofrece seguir con archivos de
     * texto. Es la diferencia entre una herramienta usable y uno de esos
     * programas que muestran una traza y se cierran.</p>
     *
     * @return los repositorios listos, o {@code null} si el usuario decide no continuar
     */
    private static Repositorios prepararAlmacen(Consola consola) {
        TipoAlmacen configurado = Configuracion.getInstancia().almacen();

        if (configurado != TipoAlmacen.MYSQL) {
            consola.aviso("Almacen configurado: " + configurado.getDescripcion());
            return FabricaRepositorios.crear(configurado);
        }

        ConexionBD conexion = ConexionBD.getInstancia();
        consola.aviso("Conectando con " + conexion.getUrl() + " como " + conexion.getUsuario() + "...");

        if (conexion.disponible()) {
            consola.exito("Conexion con MySQL establecida.");
            return FabricaRepositorios.crear(TipoAlmacen.MYSQL);
        }

        consola.saltoDeLinea();
        consola.error("No se pudo conectar con MySQL.");
        consola.mostrar("""
                  Revise que:
                    1. El servidor este encendido          (brew services start mysql)
                    2. Exista la base de datos             (mysql -u root -p < sql/crediya_db.sql)
                    3. La clave de crediya.local.properties sea la correcta
                  El detalle tecnico quedo en datos/bitacora.log""");
        consola.saltoDeLinea();

        if (consola.confirmar("  Desea continuar con archivos de texto?")) {
            return FabricaRepositorios.crear(TipoAlmacen.ARCHIVO);
        }
        return null;
    }

    private static void mostrarBanner(Consola consola) {
        consola.mostrar("""

                 ____              _ _ __   __
                / ___|_ __ ___  __| (_)\\ \\ / /_ _
               | |   | '__/ _ \\/ _` | | \\ V / _` |
               | |___| | |  __/ (_| | |  | | (_| |
                \\____|_|  \\___|\\__,_|_|  |_|\\__,_|

                 CrediYa S.A.S. - Cobros de cartera - v1.0.0
                """);
    }
}
