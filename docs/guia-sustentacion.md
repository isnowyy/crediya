# Guía de sustentación — CrediYa

Preguntas que suelen hacerse en la sustentación, con la respuesta corta y el archivo exacto
donde mostrarla en pantalla.

---

## 1. Muéstreme la herencia

**`src/main/java/com/crediya/modelo/Persona.java`**

`Persona` es abstracta y concentra lo que comparten empleados y clientes: identificador,
nombre, documento y correo, con sus validaciones. `Empleado` agrega rol y salario; `Cliente`
agrega teléfono.

Hay otras dos jerarquías que vale la pena señalar porque no son decorativas:

- `RepositorioArchivo<T>` → los cuatro repositorios de texto.
- `Menu` → los seis menús.

En ambos casos la clase base existe para **no repetir código**, no para cumplir un requisito.

---

## 2. Muéstreme el polimorfismo

Tres ejemplos, de menor a mayor peso:

1. **`Persona.descripcionRol()`** — `Empleado` devuelve el cargo y el salario; `Cliente`
   devuelve su teléfono. El mismo mensaje, dos respuestas.
2. **`CalculadoraInteres.calcular()`** — tres implementaciones con fórmulas distintas.
   `Prestamo` las llama sin saber cuál le tocó.
3. **`Repositorio<T>`** — tres implementaciones (MySQL, archivo, memoria). Los servicios
   funcionan igual con cualquiera. **Este es el ejemplo fuerte**: demuestra polimorfismo e
   inversión de dependencias al mismo tiempo.

---

## 3. ¿Dónde está el encapsulamiento?

Todos los atributos del modelo son privados y los modificadores validan antes de asignar:

```java
public void setCorreo(String correo) {
    this.correo = Validaciones.correo(correo);   // lanza si no es válido
}
```

El caso más interesante es `Prestamo.getPagos()`:

```java
return Collections.unmodifiableList(pagos);
```

Si devolviera la lista directamente, cualquiera podría agregar un pago saltándose las
validaciones de `registrarPago()`. El encapsulamiento no es poner `private`: es que **no
exista forma de dejar el objeto en un estado inválido**.

---

## 4. ¿Por qué `BigDecimal` y no `double`?

Porque `0.1 + 0.2` en punto flotante no da `0.3`. En una cartera eso significa saldos que no
cierran. Todo el dinero pasa por `util/Dinero`, con dos decimales y redondeo `HALF_UP`.

Demostración rápida en consola si lo piden:

```java
System.out.println(0.1 + 0.2);   // 0.30000000000000004
```

---

## 5. Muéstreme las lambdas y los Streams

**`servicio/ReporteServicio.java`** es el módulo escrito para eso. Un ejemplo completo:

```java
public List<ClienteMoroso> clientesMorosos(LocalDate referencia) {
    Map<Cliente, List<Prestamo>> vencidosPorCliente = prestamosVencidos(referencia).stream()
            .collect(Collectors.groupingBy(Prestamo::getCliente));

    return vencidosPorCliente.entrySet().stream()
            .map(entrada -> new ClienteMoroso(
                    entrada.getKey(),
                    entrada.getValue().size(),
                    sumar(entrada.getValue(), Prestamo::getSaldoPendiente),
                    entrada.getValue().stream()
                            .mapToLong(prestamo -> prestamo.diasDeMora(referencia))
                            .max()
                            .orElse(0L)))
            .sorted(Comparator.comparing(ClienteMoroso::saldoTotal).reversed())
            .toList();
}
```

Y un uso de lambda que no es un reporte: **los menús son datos, no `switch`**.

```java
return List.of(
        OpcionMenu.de("Registrar empleado", this::registrar),
        OpcionMenu.de("Listar empleados",   this::listar),
        ...);
```

Agregar una opción es agregar una línea a una lista.

---

## 6. ¿Por qué los reportes se calculan en Java y no con `GROUP BY` en SQL?

Porque entonces solo funcionarían con MySQL. El sistema tiene tres almacenes y los reportes
deben dar exactamente el mismo resultado en los tres. Además, el enunciado pide
explícitamente Stream API.

La vista `v_saldo_prestamos` del script SQL existe justamente para poder **contrastar** desde
la consola de MySQL que los números coinciden.

---

## 7. Muéstreme el JDBC

**`repositorio/jdbc/RepositorioJdbc.java`** — ahí está el ceremonial completo, escrito una
sola vez:

```java
try (Connection conexion = conexionBD.abrir();
     PreparedStatement sentencia = conexion.prepareStatement(sql)) {
    aplicarParametros(sentencia, parametros);
    try (ResultSet resultado = sentencia.executeQuery()) { ... }
} catch (SQLException e) {
    throw new PersistenciaException("Fallo la consulta sobre " + tabla(), e);
}
```

Tres puntos que vale la pena resaltar:

- **try-with-resources**: ninguna conexión queda abierta, ni siquiera si hay excepción.
- **`PreparedStatement` siempre**: ningún dato del usuario se concatena dentro del SQL. Eso
  es lo que evita la inyección SQL.
- **Traducción de la excepción**: `SQLException` se envuelve en `PersistenciaException` para
  que los servicios no queden atados a JDBC.

Y en `JdbcPrestamoRepositorio`: la consulta base trae préstamo + cliente + empleado con
`JOIN`, y `completar()` reparte los pagos con **una sola** consulta adicional. Son dos
consultas en total, no `3N + 1`.

---

## 8. ¿Qué pasa si meto un dato malo?

Pruébelo en vivo: en "Registrar empleado", escriba un correo sin arroba.

```
  Correo: pepito
  [!] El correo 'pepito' no tiene un formato valido.
  Correo:
```

Vuelve a pedirlo, no se cae. Eso ocurre en `Consola.insistir()`, que reintenta ante
`ValidacionException`.

Y si la regla es de negocio, el mensaje explica el porqué:

```
  [!] El abono de $ 2.000.000,00 supera el saldo pendiente de $ 1.080.000,00.
```

Todas las excepciones se atrapan en un solo sitio: `Menu.ejecutarConRed()`.

---

## 9. ¿Cómo demuestro que el diseño está desacoplado?

El argumento más contundente son las **pruebas unitarias**:

```bash
mvn test
```

59 pruebas que corren **sin MySQL instalado y sin tocar un solo archivo del proyecto**. Los
servicios reciben repositorios en memoria y no se enteran. Si los servicios dependieran de
JDBC, esas pruebas serían imposibles.

El segundo argumento: `./run.sh memoria`, `./run.sh archivo` y `./run.sh mysql` ejecutan la
**misma** aplicación sobre tres almacenes distintos. Lo único que cambia es una línea de
configuración, porque `FabricaRepositorios` es el único lugar donde se nombran las clases
concretas.

---

## 10. ¿Por qué `vencido` no es un estado en la base de datos?

Porque no es algo que alguien escriba, sino algo que se concluye:

```java
public boolean estaVencido(LocalDate referencia) {
    return estado == EstadoPrestamo.PENDIENTE
            && getSaldoPendiente().signum() > 0
            && referencia.isAfter(getFechaVencimiento());
}
```

Guardarlo obligaría a un proceso nocturno que lo recalcule, y a vivir con el riesgo de que
quede desactualizado. Un dato derivado se calcula, no se almacena.

---

## 11. ¿Qué le falta al sistema?

Responder esto con honestidad suma puntos. Está en el README, sección *Alcance y
limitaciones conocidas*:

- El abono y la actualización del préstamo no van en una transacción.
- Los repositorios de archivo reescriben el archivo completo en cada operación.
- No hay pool de conexiones.
- No hay autenticación de usuarios.

En los cuatro casos la decisión fue consciente y está justificada por el alcance del
ejercicio.

---

## Guion de demostración en 5 minutos

```bash
./run.sh memoria
```

1. **Opción 6** → cargar datos de demostración (23 registros).
2. **Opción 7** → estado del sistema: cuántos hay de cada cosa.
3. **3 → 1** → simular un crédito. Mostrar que calcula total, intereses y cuota.
4. **5 → 1** → resumen de cartera. Señalar el índice de mora.
5. **5 → 4** → clientes morosos.
6. **3 → 2** → intentar prestarle al cliente moroso: el sistema lo niega explicando por qué.
7. **4 → 1** → abonar a un préstamo. Mostrar cómo baja el saldo.
8. Intentar abonar más de lo que se debe: el sistema lo rechaza.
9. **5 → 9** → exportar a archivos de texto y abrir `datos/reporte-cartera.txt`.
10. En otra terminal: `mvn test` → 59 pruebas en verde.
