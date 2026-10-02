package com.crediya.servicio;

import com.crediya.config.FabricaRepositorios;
import com.crediya.config.Repositorios;
import com.crediya.excepcion.PersistenciaException;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.servicio.reporte.ClienteMoroso;
import com.crediya.servicio.reporte.ProductividadEmpleado;
import com.crediya.servicio.reporte.ResumenCartera;
import com.crediya.util.Bitacora;
import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vuelca el contenido del almacen activo a archivos de texto.
 *
 * <p>Es el puente entre las dos formas de persistencia que pide el enunciado:
 * aunque el sistema este trabajando contra MySQL, desde el menu se puede
 * generar en cualquier momento el juego de {@code .txt} con el estado actual.
 * No duplica el formato de escritura, sino que reutiliza los repositorios de
 * archivo: exportar es, literalmente, guardar lo mismo en el otro almacen.</p>
 */
public class ExportadorTexto {

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Repositorios origen;
    private final ReporteServicio reporteServicio;

    public ExportadorTexto(Repositorios origen, ReporteServicio reporteServicio) {
        this.origen = Validaciones.noNulo(origen, "repositorios de origen");
        this.reporteServicio = Validaciones.noNulo(reporteServicio, "servicio de reportes");
    }

    /**
     * Escribe empleados.txt, clientes.txt, prestamos.txt, pagos.txt y
     * reporte-cartera.txt en la carpeta indicada.
     *
     * @return rutas de los archivos generados
     */
    public List<Path> exportarTodo(Path carpeta) {
        Validaciones.noNulo(carpeta, "carpeta de destino");

        // Se lee todo ANTES de borrar: si el almacen activo ya fuera el de
        // archivos, borrar primero destruiria justo lo que se quiere exportar.
        List<Empleado> empleados = origen.empleados().listar();
        List<Cliente> clientes = origen.clientes().listar();
        List<Pago> pagos = origen.pagos().listar();
        List<Prestamo> prestamos = origen.prestamos().listar();

        borrar(carpeta, "empleados.txt", "clientes.txt", "prestamos.txt", "pagos.txt");

        Repositorios destino = FabricaRepositorios.crearArchivo(carpeta);

        // El orden importa: el repositorio de prestamos necesita poder resolver
        // sus clientes, empleados y pagos mientras escribe.
        empleados.forEach(destino.empleados()::guardar);
        clientes.forEach(destino.clientes()::guardar);
        pagos.forEach(destino.pagos()::guardar);
        prestamos.forEach(destino.prestamos()::guardar);

        Path reporte = escribirReporte(carpeta.resolve("reporte-cartera.txt"));

        Bitacora.info("Exportacion a texto completada en " + carpeta.toAbsolutePath());

        return List.of(
                carpeta.resolve("empleados.txt"),
                carpeta.resolve("clientes.txt"),
                carpeta.resolve("prestamos.txt"),
                carpeta.resolve("pagos.txt"),
                reporte);
    }

    /** Informe de cartera en texto plano, listo para imprimir o adjuntar. */
    private Path escribirReporte(Path archivo) {
        ResumenCartera resumen = reporteServicio.resumenCartera();

        StringBuilder contenido = new StringBuilder()
                .append("================================================================\n")
                .append(" CrediYa S.A.S. - Informe de cartera\n")
                .append(" Generado el ").append(LocalDateTime.now().format(SELLO)).append('\n')
                .append("================================================================\n\n")
                .append("RESUMEN GENERAL\n")
                .append("----------------------------------------------------------------\n")
                .append(resumen.comoTexto()).append("\n\n");

        contenido.append("CLIENTES MOROSOS\n")
                .append("----------------------------------------------------------------\n");
        List<ClienteMoroso> morosos = reporteServicio.clientesMorosos();
        if (morosos.isEmpty()) {
            contenido.append("Sin cartera vencida a la fecha.\n");
        } else {
            morosos.forEach(moroso -> contenido.append(moroso.fila()).append('\n'));
        }

        contenido.append("\nPRODUCTIVIDAD POR EMPLEADO\n")
                .append("----------------------------------------------------------------\n");
        for (ProductividadEmpleado productividad : reporteServicio.productividadPorEmpleado()) {
            contenido.append(productividad.fila()).append('\n');
        }

        contenido.append("\nPRESTAMOS ACTIVOS\n")
                .append("----------------------------------------------------------------\n");
        List<Prestamo> activos = reporteServicio.prestamosActivos();
        if (activos.isEmpty()) {
            contenido.append("No hay prestamos activos.\n");
        } else {
            activos.forEach(prestamo -> contenido
                    .append(String.format("#%-4d %-26s vence %s  saldo %s%n",
                            prestamo.getId(), prestamo.getCliente().getNombre(),
                            prestamo.getFechaVencimiento(),
                            Dinero.formatear(prestamo.getSaldoPendiente()))));
        }

        try {
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, contenido.toString(), StandardCharsets.UTF_8);
            return archivo;
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo escribir el informe en " + archivo, e);
        }
    }

    private void borrar(Path carpeta, String... nombres) {
        try {
            Files.createDirectories(carpeta);
            for (String nombre : nombres) {
                Files.deleteIfExists(carpeta.resolve(nombre));
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo preparar la carpeta " + carpeta, e);
        }
    }
}
