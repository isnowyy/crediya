package com.crediya.modelo;

import com.crediya.util.Dinero;
import com.crediya.util.Validaciones;

import java.math.BigDecimal;

/**
 * Funcionario de CrediYa. Es quien coloca los prestamos y gestiona los cobros.
 */
public class Empleado extends Persona {

    private Rol rol;
    private BigDecimal salario;

    public Empleado(Integer id, String nombre, String documento, Rol rol, String correo, BigDecimal salario) {
        super(id, nombre, documento, correo);
        setRol(rol);
        setSalario(salario);
    }

    /** Constructor para empleados que todavia no se han guardado. */
    public Empleado(String nombre, String documento, Rol rol, String correo, BigDecimal salario) {
        this(null, nombre, documento, rol, correo, salario);
    }

    @Override
    public String descripcionRol() {
        return rol.getDescripcion() + " (salario " + Dinero.formatear(salario) + ")";
    }

    @Override
    public String tipo() {
        return "EMPLEADO";
    }

    /** Comision que este empleado gana por colocar un prestamo del monto dado. */
    public BigDecimal comisionPor(BigDecimal montoColocado) {
        return rol.comisionSobre(montoColocado);
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = Validaciones.noNulo(rol, "rol");
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = Validaciones.montoPositivo(salario, "salario");
    }
}
