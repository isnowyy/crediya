package com.crediya.util;

import com.crediya.excepcion.ValidacionException;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Toda la entrada y salida por consola del sistema.
 *
 * <p>Esta clase existe para que ninguna otra tenga que tocar {@code Scanner} ni
 * {@code System.out}. Los menus piden "un entero entre 1 y 5" y reciben un
 * entero entre 1 y 5; la insistencia al usuario, el reintento y los mensajes de
 * error se resuelven una sola vez aqui.</p>
 *
 * <p>Se usa un unico {@code Scanner} sobre {@code System.in} durante toda la
 * vida del programa: abrir varios sobre el mismo flujo hace que uno se coma lo
 * que el otro esperaba.</p>
 */
public class Consola implements AutoCloseable {

    private static final String PREFIJO_ERROR = "  [!] ";

    private final Scanner scanner;
    private final PrintStream salida;

    public Consola() {
        this(new Scanner(System.in), System.out);
    }

    /** Constructor para pruebas: permite inyectar entrada y salida simuladas. */
    public Consola(Scanner scanner, PrintStream salida) {
        this.scanner = scanner;
        this.salida = salida;
    }

    // ------------------------------------------------------------------
    // Salida
    // ------------------------------------------------------------------

    public void mostrar(String texto) {
        salida.println(texto);
    }

    public void mostrar(String formato, Object... argumentos) {
        salida.printf(formato + "%n", argumentos);
    }

    public void saltoDeLinea() {
        salida.println();
    }

    public void titulo(String texto) {
        String linea = "=".repeat(Math.max(texto.length() + 4, 64));
        salida.println();
        salida.println(linea);
        salida.println("  " + texto.toUpperCase());
        salida.println(linea);
    }

    public void subtitulo(String texto) {
        salida.println();
        salida.println("-- " + texto);
        salida.println("-".repeat(64));
    }

    public void exito(String texto) {
        salida.println("  [ok] " + texto);
    }

    public void error(String texto) {
        salida.println(PREFIJO_ERROR + texto);
    }

    public void aviso(String texto) {
        salida.println("  [i] " + texto);
    }

    /** Imprime una lista numerada, o un mensaje cuando no hay nada que mostrar. */
    public <T> void listar(List<T> elementos, Function<T, String> comoTexto, String mensajeVacio) {
        if (elementos.isEmpty()) {
            aviso(mensajeVacio);
            return;
        }
        for (int i = 0; i < elementos.size(); i++) {
            salida.printf("  %2d. %s%n", i + 1, comoTexto.apply(elementos.get(i)));
        }
        salida.printf("  (%d registro%s)%n", elementos.size(), elementos.size() == 1 ? "" : "s");
    }

    // ------------------------------------------------------------------
    // Entrada
    // ------------------------------------------------------------------

    /**
     * Lee una linea cruda.
     *
     * @throws EntradaCerradaException cuando ya no hay mas entrada que leer
     */
    public String leerLinea(String etiqueta) {
        salida.print(etiqueta);
        salida.flush();

        if (!scanner.hasNextLine()) {
            salida.println();
            throw new EntradaCerradaException("Se agoto la entrada estandar.");
        }
        return scanner.nextLine().trim();
    }

    public String leerTexto(String etiqueta) {
        return insistir(() -> Validaciones.textoObligatorio(leerLinea(etiqueta), "dato"));
    }

    /** Lee un texto que puede quedar vacio; devuelve {@code null} si lo esta. */
    public String leerTextoOpcional(String etiqueta) {
        String valor = leerLinea(etiqueta);
        return valor.isBlank() ? null : valor;
    }

    public int leerEntero(String etiqueta) {
        return insistir(() -> {
            String valor = leerLinea(etiqueta);
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                throw new ValidacionException("'" + valor + "' no es un numero entero.");
            }
        });
    }

    public int leerEntero(String etiqueta, int minimo, int maximo) {
        return insistir(() -> Validaciones.enteroEnRango(leerEntero(etiqueta), minimo, maximo, "valor"));
    }

    public BigDecimal leerDecimal(String etiqueta) {
        return insistir(() -> {
            String valor = leerLinea(etiqueta).replace(",", ".").replace("_", "");
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException e) {
                throw new ValidacionException("'" + valor + "' no es un numero valido.");
            }
        });
    }

    /** Lee una fecha en formato aaaa-mm-dd; una linea vacia significa hoy. */
    public LocalDate leerFecha(String etiqueta) {
        return insistir(() -> {
            String valor = leerLinea(etiqueta);
            if (valor.isBlank()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(valor);
            } catch (DateTimeParseException e) {
                throw new ValidacionException("Use el formato aaaa-mm-dd, por ejemplo 2026-03-15.");
            }
        });
    }

    /** Muestra las constantes de un enum numeradas y devuelve la elegida. */
    public <E extends Enum<E>> E leerOpcionEnum(String etiqueta, Class<E> tipo, Function<E, String> descripcion) {
        List<E> constantes = Arrays.asList(tipo.getEnumConstants());

        saltoDeLinea();
        for (int i = 0; i < constantes.size(); i++) {
            salida.printf("    %d) %-14s %s%n", i + 1,
                    constantes.get(i).name(), descripcion.apply(constantes.get(i)));
        }
        return constantes.get(leerEntero(etiqueta, 1, constantes.size()) - 1);
    }

    public boolean confirmar(String pregunta) {
        String respuesta = leerLinea(pregunta + " (s/n): ").toLowerCase();
        return respuesta.startsWith("s");
    }

    public void pausa() {
        leerLinea("\n  Pulse Enter para continuar...");
    }

    @Override
    public void close() {
        scanner.close();
    }

    /**
     * Repite la lectura hasta que el dato sea valido.
     *
     * <p>Atrapa solo {@link ValidacionException}: cualquier otro problema debe
     * subir, y {@link EntradaCerradaException} en particular, porque si no
     * habria quedado un ciclo infinito pidiendo un dato que ya nadie va a
     * escribir.</p>
     */
    private <T> T insistir(java.util.function.Supplier<T> lectura) {
        while (true) {
            try {
                return lectura.get();
            } catch (ValidacionException e) {
                error(e.getMessage());
            }
        }
    }
}
