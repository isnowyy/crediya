package com.crediya.modelo;

import com.crediya.util.Validaciones;

import java.util.Objects;

/**
 * Clase base de las personas que participan en la cartera.
 *
 * <p>Concentra el estado y las validaciones comunes de {@link Empleado} y
 * {@link Cliente} (herencia) y obliga a cada subclase a describirse a si misma
 * (polimorfismo). Los atributos son privados y solo se tocan por sus
 * modificadores, que validan antes de asignar (encapsulamiento).</p>
 */
public abstract class Persona implements Identificable {

    private Integer id;
    private String nombre;
    private String documento;
    private String correo;

    protected Persona(Integer id, String nombre, String documento, String correo) {
        this.id = id;
        setNombre(nombre);
        setDocumento(documento);
        setCorreo(correo);
    }

    /**
     * Describe el papel de la persona dentro de CrediYa.
     * Cada subclase responde distinto: es el punto de polimorfismo del modelo.
     */
    public abstract String descripcionRol();

    /** Etiqueta corta del tipo de persona, util en listados y archivos. */
    public abstract String tipo();

    /** Linea legible para los menus de consola. Usa {@link #descripcionRol()}. */
    public String resumen() {
        return String.format("#%s %s (doc. %s) - %s", id, nombre, documento, descripcionRol());
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = Validaciones.nombre(nombre);
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = Validaciones.documento(documento);
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = Validaciones.correo(correo);
    }

    /**
     * Dos personas son la misma si comparten documento y tipo.
     * El identificador no sirve para comparar porque una entidad recien creada
     * aun no lo tiene.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Persona persona)) {
            return false;
        }
        return getClass() == otro.getClass() && documento.equals(persona.documento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), documento);
    }

    @Override
    public String toString() {
        return resumen();
    }
}
