package com.crediya.servicio;

import com.crediya.excepcion.RecursoNoEncontradoException;
import com.crediya.excepcion.ReglaNegocioException;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Prestamo;
import com.crediya.repositorio.ClienteRepositorio;
import com.crediya.repositorio.PrestamoRepositorio;
import com.crediya.util.Bitacora;
import com.crediya.util.Validaciones;

import java.util.Comparator;
import java.util.List;

/**
 * Reglas de negocio sobre los clientes y su relacion con los prestamos.
 */
public class ClienteServicio {

    private final ClienteRepositorio repositorio;
    private final PrestamoRepositorio prestamoRepositorio;

    public ClienteServicio(ClienteRepositorio repositorio, PrestamoRepositorio prestamoRepositorio) {
        this.repositorio = Validaciones.noNulo(repositorio, "repositorio de clientes");
        this.prestamoRepositorio = Validaciones.noNulo(prestamoRepositorio, "repositorio de prestamos");
    }

    /**
     * Registra un cliente nuevo.
     *
     * @throws ReglaNegocioException si ya existe otro con el mismo documento
     */
    public Cliente registrar(String nombre, String documento, String correo, String telefono) {
        Cliente cliente = new Cliente(nombre, documento, correo, telefono);

        repositorio.buscarPorDocumento(cliente.getDocumento()).ifPresent(existente -> {
            throw new ReglaNegocioException(
                    "Ya hay un cliente con el documento " + existente.getDocumento()
                            + ": " + existente.getNombre() + ".");
        });

        Cliente guardado = repositorio.guardar(cliente);
        Bitacora.info("Cliente registrado: #" + guardado.getId() + " " + guardado.getNombre());
        return guardado;
    }

    public List<Cliente> listar() {
        return repositorio.listar();
    }

    public Cliente buscarPorId(int id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", id));
    }

    public Cliente buscarPorDocumento(String documento) {
        String limpio = Validaciones.documento(documento);
        return repositorio.buscarPorDocumento(limpio)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente con documento", limpio));
    }

    /**
     * Prestamos de un cliente, del mas reciente al mas antiguo.
     * Verifica primero que el cliente exista para poder dar un mensaje claro.
     */
    public List<Prestamo> prestamosDe(int clienteId) {
        Cliente cliente = buscarPorId(clienteId);
        return prestamoRepositorio.buscarPorCliente(cliente.getId()).stream()
                .sorted(Comparator.comparing(Prestamo::getFechaInicio).reversed())
                .toList();
    }

    /** Busca por coincidencia parcial de nombre, sin distinguir mayusculas. */
    public List<Cliente> buscarPorNombre(String fragmento) {
        String texto = Validaciones.textoObligatorio(fragmento, "nombre").toLowerCase();
        return repositorio.listar().stream()
                .filter(cliente -> cliente.getNombre().toLowerCase().contains(texto))
                .sorted(Comparator.comparing(Cliente::getNombre))
                .toList();
    }
}
