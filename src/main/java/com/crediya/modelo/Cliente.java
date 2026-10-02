package com.crediya.modelo;

import com.crediya.util.Validaciones;

/**
 * Persona a la que CrediYa le presta dinero.
 */
public class Cliente extends Persona {

    private String telefono;

    public Cliente(Integer id, String nombre, String documento, String correo, String telefono) {
        super(id, nombre, documento, correo);
        setTelefono(telefono);
    }

    /** Constructor para clientes que todavia no se han guardado. */
    public Cliente(String nombre, String documento, String correo, String telefono) {
        this(null, nombre, documento, correo, telefono);
    }

    @Override
    public String descripcionRol() {
        return "Cliente, tel. " + telefono;
    }

    @Override
    public String tipo() {
        return "CLIENTE";
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = Validaciones.telefono(telefono);
    }
}
