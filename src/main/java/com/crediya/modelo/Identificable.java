package com.crediya.modelo;

/**
 * Contrato minimo que debe cumplir cualquier entidad que se pueda guardar.
 *
 * <p>Es lo unico que {@code Repositorio<T>} necesita saber de una entidad para
 * poder asignarle un identificador y buscarla despues. Mantener la interfaz en
 * dos metodos es aplicar segregacion de interfaces (la I de SOLID).</p>
 */
public interface Identificable {

    /** Identificador asignado por el almacen, o {@code null} si aun no se ha guardado. */
    Integer getId();

    void setId(Integer id);

    /** Verdadero cuando la entidad todavia no existe en el almacen. */
    default boolean esNueva() {
        return getId() == null;
    }
}
