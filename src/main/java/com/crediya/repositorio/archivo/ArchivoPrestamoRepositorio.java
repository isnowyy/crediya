package com.crediya.repositorio.archivo;

import com.crediya.excepcion.PersistenciaException;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.TipoInteres;
import com.crediya.repositorio.ClienteRepositorio;
import com.crediya.repositorio.EmpleadoRepositorio;
import com.crediya.repositorio.PagoRepositorio;
import com.crediya.repositorio.PrestamoRepositorio;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prestamos guardados en {@code datos/prestamos.txt}.
 *
 * <p>El archivo solo guarda los identificadores del cliente y del empleado, como
 * lo haria una tabla relacional. Este repositorio se encarga de volver a unir
 * las piezas: pide las personas a sus propios repositorios y el historico al de
 * pagos, y entrega el prestamo completo. Depende de las interfaces, nunca de las
 * implementaciones concretas, asi que funciona igual de bien con repositorios de
 * archivo o de base de datos.</p>
 */
public class ArchivoPrestamoRepositorio extends RepositorioArchivo<Prestamo> implements PrestamoRepositorio {

    private final ClienteRepositorio clienteRepositorio;
    private final EmpleadoRepositorio empleadoRepositorio;
    private final PagoRepositorio pagoRepositorio;

    /**
     * Indices temporales que viven solo mientras dura una lectura completa.
     *
     * <p>Sin ellos, reconstruir N prestamos releeria los archivos de clientes y
     * empleados N veces (el clasico problema N+1). Se llenan al entrar a
     * {@link #listar()} y se descartan al salir.</p>
     */
    private Map<Integer, Cliente> indiceClientes;
    private Map<Integer, Empleado> indiceEmpleados;

    public ArchivoPrestamoRepositorio(Path archivo,
                                      ClienteRepositorio clienteRepositorio,
                                      EmpleadoRepositorio empleadoRepositorio,
                                      PagoRepositorio pagoRepositorio) {
        super(archivo);
        this.clienteRepositorio = clienteRepositorio;
        this.empleadoRepositorio = empleadoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
    }

    @Override
    protected String cabecera() {
        return "id;cliente_id;empleado_id;monto;interes;cuotas;fecha_inicio;estado;tipo_interes";
    }

    @Override
    protected String serializar(Prestamo prestamo) {
        return String.join(SEPARADOR,
                String.valueOf(prestamo.getId()),
                String.valueOf(prestamo.getCliente().getId()),
                String.valueOf(prestamo.getEmpleado().getId()),
                prestamo.getMonto().toPlainString(),
                prestamo.getTasaInteresMensual().toPlainString(),
                String.valueOf(prestamo.getCuotas()),
                prestamo.getFechaInicio().toString(),
                prestamo.getEstado().name(),
                prestamo.getTipoInteres().name());
    }

    @Override
    protected Prestamo deserializar(String[] campos) {
        int clienteId = Integer.parseInt(campos[1].trim());
        int empleadoId = Integer.parseInt(campos[2].trim());

        return Prestamo.builder()
                .id(Integer.valueOf(campos[0].trim()))
                .cliente(resolverCliente(clienteId))
                .empleado(resolverEmpleado(empleadoId))
                .monto(new BigDecimal(campos[3].trim()))
                .tasaInteresMensual(new BigDecimal(campos[4].trim()))
                .cuotas(Integer.parseInt(campos[5].trim()))
                .fechaInicio(LocalDate.parse(campos[6].trim()))
                .estado(EstadoPrestamo.desde(campos[7]))
                .tipoInteres(TipoInteres.desde(campos[8]))
                .construir();
    }

    @Override
    public List<Prestamo> listar() {
        abrirIndices();
        try {
            return conHistorico(super.listar());
        } finally {
            cerrarIndices();
        }
    }

    @Override
    public Optional<Prestamo> buscarPorId(int id) {
        return listar().stream()
                .filter(prestamo -> Integer.valueOf(id).equals(prestamo.getId()))
                .findFirst();
    }

    @Override
    public List<Prestamo> buscarPorCliente(int clienteId) {
        return listar().stream()
                .filter(prestamo -> clienteId == prestamo.getCliente().getId())
                .toList();
    }

    @Override
    public List<Prestamo> buscarPorEmpleado(int empleadoId) {
        return listar().stream()
                .filter(prestamo -> empleadoId == prestamo.getEmpleado().getId())
                .toList();
    }

    // ------------------------------------------------------------------
    // Reconstruccion de los objetos
    // ------------------------------------------------------------------

    /** Reparte los pagos entre sus prestamos con una sola lectura del archivo. */
    private List<Prestamo> conHistorico(List<Prestamo> prestamos) {
        Map<Integer, List<Pago>> pagosPorPrestamo = pagoRepositorio.listar().stream()
                .sorted(Comparator.comparing(Pago::getFechaPago))
                .collect(Collectors.groupingBy(Pago::getPrestamoId));

        prestamos.forEach(prestamo ->
                prestamo.cargarHistorico(pagosPorPrestamo.getOrDefault(prestamo.getId(), List.of())));

        return prestamos;
    }

    private Cliente resolverCliente(int clienteId) {
        if (indiceClientes != null && indiceClientes.containsKey(clienteId)) {
            return indiceClientes.get(clienteId);
        }
        return clienteRepositorio.buscarPorId(clienteId)
                .orElseThrow(() -> new PersistenciaException(
                        "El archivo de prestamos referencia al cliente " + clienteId + ", que no existe."));
    }

    private Empleado resolverEmpleado(int empleadoId) {
        if (indiceEmpleados != null && indiceEmpleados.containsKey(empleadoId)) {
            return indiceEmpleados.get(empleadoId);
        }
        return empleadoRepositorio.buscarPorId(empleadoId)
                .orElseThrow(() -> new PersistenciaException(
                        "El archivo de prestamos referencia al empleado " + empleadoId + ", que no existe."));
    }

    private void abrirIndices() {
        indiceClientes = clienteRepositorio.listar().stream()
                .collect(Collectors.toMap(Cliente::getId, Function.identity()));
        indiceEmpleados = empleadoRepositorio.listar().stream()
                .collect(Collectors.toMap(Empleado::getId, Function.identity()));
    }

    private void cerrarIndices() {
        indiceClientes = null;
        indiceEmpleados = null;
    }
}
