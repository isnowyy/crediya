package com.crediya.app;

import com.crediya.config.Repositorios;
import com.crediya.modelo.Cliente;
import com.crediya.modelo.Empleado;
import com.crediya.modelo.EstadoPrestamo;
import com.crediya.modelo.Pago;
import com.crediya.modelo.Prestamo;
import com.crediya.modelo.Rol;
import com.crediya.modelo.TipoInteres;
import com.crediya.util.Bitacora;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga un juego de datos de ejemplo en el almacen activo.
 *
 * <p>Son los mismos registros que inserta {@code sql/crediya_db.sql}, para que
 * el sistema se vea igual trabajando contra MySQL o contra archivos. Las fechas
 * se calculan hacia atras desde hoy, de modo que siempre haya prestamos al dia
 * y prestamos vencidos sin importar cuando se ejecute la demostracion.</p>
 *
 * <p>Los abonos se aplican con {@link Prestamo#registrarPago(Pago)} y no
 * insertandolos a mano: asi los datos de ejemplo pasan por las mismas reglas de
 * negocio que cualquier pago real y no puede quedar sembrado un estado
 * imposible.</p>
 */
public final class DatosDemo {

    private DatosDemo() {
        // Utilidad: no se instancia.
    }

    /**
     * @return cuantos registros se crearon en total
     */
    public static int sembrar(Repositorios repositorios) {
        LocalDate hoy = LocalDate.now();

        List<Empleado> empleados = List.of(
                new Empleado("Ana Maria Gomez", "1098765432", Rol.ASESOR,
                        "ana.gomez@crediya.co", new BigDecimal("3200000")),
                new Empleado("Carlos Pena Ruiz", "91234567", Rol.COBRADOR,
                        "carlos.pena@crediya.co", new BigDecimal("2400000")),
                new Empleado("Laura Rincon", "1020304050", Rol.ANALISTA,
                        "laura.rincon@crediya.co", new BigDecimal("3800000")),
                new Empleado("Jorge Villamizar", "79856231", Rol.ADMINISTRADOR,
                        "jorge.v@crediya.co", new BigDecimal("6500000")));

        List<Cliente> clientes = List.of(
                new Cliente("Pedro Sanchez Diaz", "1005432198", "pedro.sanchez@correo.com", "3105558877"),
                new Cliente("Marta Quintero", "63301122", "marta.q@correo.com", "3009991122"),
                new Cliente("Luis Fernando Ariza", "1090443322", "lf.ariza@correo.com", "3187774455"),
                new Cliente("Sofia Carreno", "1101223344", "sofia.carreno@correo.com", "3024446677"),
                new Cliente("Diego Mantilla", "88112233", "diego.mantilla@correo.com", "3156663322"));

        empleados.forEach(repositorios.empleados()::guardar);
        clientes.forEach(repositorios.clientes()::guardar);

        int creados = empleados.size() + clientes.size();

        // cliente, empleado, capital, interes mensual, cuotas, meses de antiguedad, modalidad
        creados += otorgar(repositorios, clientes.get(0), empleados.get(0),
                "5000000", "2.00", 12, 3, TipoInteres.SIMPLE, hoy,
                List.of("516666.67", "516666.67"));

        creados += otorgar(repositorios, clientes.get(1), empleados.get(0),
                "2000000", "2.50", 6, 9, TipoInteres.SIMPLE, hoy,
                List.of("383333.33"));

        creados += otorgar(repositorios, clientes.get(2), empleados.get(1),
                "8000000", "1.80", 24, 2, TipoInteres.CUOTA_FIJA, hoy,
                List.of("412000.00"));

        creados += otorgar(repositorios, clientes.get(3), empleados.get(2),
                "1500000", "3.00", 4, 10, TipoInteres.COMPUESTO, hoy,
                List.of("200000.00"));

        creados += otorgar(repositorios, clientes.get(4), empleados.get(1),
                "3000000", "2.20", 6, 8, TipoInteres.SIMPLE, hoy,
                List.of("1700000.00", "1696000.00"));

        creados += otorgar(repositorios, clientes.get(0), empleados.get(2),
                "12000000", "1.50", 36, 1, TipoInteres.CUOTA_FIJA, hoy,
                List.of("420000.00"));

        Bitacora.info("Datos de demostracion sembrados: " + creados + " registros.");
        return creados;
    }

    /**
     * Crea un prestamo con fecha hacia atras y le aplica sus abonos.
     *
     * @return cuantos registros se crearon (el prestamo mas sus pagos)
     */
    private static int otorgar(Repositorios repositorios,
                               Cliente cliente,
                               Empleado empleado,
                               String capital,
                               String interes,
                               int cuotas,
                               int mesesDeAntiguedad,
                               TipoInteres tipo,
                               LocalDate hoy,
                               List<String> abonos) {

        Prestamo prestamo = Prestamo.builder()
                .cliente(cliente)
                .empleado(empleado)
                .monto(new BigDecimal(capital))
                .tasaInteresMensual(new BigDecimal(interes))
                .cuotas(cuotas)
                .fechaInicio(hoy.minusMonths(mesesDeAntiguedad))
                .tipoInteres(tipo)
                .estado(EstadoPrestamo.PENDIENTE)
                .construir();

        repositorios.prestamos().guardar(prestamo);

        int mesesAtras = mesesDeAntiguedad;
        for (String abono : abonos) {
            mesesAtras = Math.max(mesesAtras - 1, 0);

            Pago pago = new Pago(null, prestamo.getId(), hoy.minusMonths(mesesAtras), new BigDecimal(abono));
            prestamo.registrarPago(pago);
            repositorios.pagos().guardar(pago);
        }

        // Se vuelve a guardar para persistir el estado, que pudo pasar a PAGADO.
        repositorios.prestamos().guardar(prestamo);

        return 1 + abonos.size();
    }
}
