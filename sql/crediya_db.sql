-- ============================================================================
--  CrediYa S.A.S. - Esquema de la base de datos
--  Motor: MySQL 8.0 o superior
--
--  Ejecucion:  mysql -u root -p < sql/crediya_db.sql
--
--  Este script es idempotente: puede ejecutarse varias veces sin error.
-- ============================================================================

DROP DATABASE IF EXISTS crediya_db;
CREATE DATABASE crediya_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE crediya_db;

-- ----------------------------------------------------------------------------
-- EMPLEADOS
-- El documento y el correo son identificadores naturales: se marcan UNIQUE para
-- que la propia base de datos impida duplicados aunque la aplicacion falle.
-- ----------------------------------------------------------------------------
CREATE TABLE empleados (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  nombre    VARCHAR(80)    NOT NULL,
  documento VARCHAR(30)    NOT NULL,
  rol       VARCHAR(30)    NOT NULL,
  correo    VARCHAR(80)    NOT NULL,
  salario   DECIMAL(12,2)  NOT NULL,

  CONSTRAINT uq_empleados_documento UNIQUE (documento),
  CONSTRAINT uq_empleados_correo    UNIQUE (correo),
  CONSTRAINT ck_empleados_salario   CHECK (salario > 0),
  CONSTRAINT ck_empleados_rol       CHECK (rol IN ('ASESOR','COBRADOR','ANALISTA','ADMINISTRADOR'))
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- CLIENTES
-- ----------------------------------------------------------------------------
CREATE TABLE clientes (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  nombre    VARCHAR(80) NOT NULL,
  documento VARCHAR(30) NOT NULL,
  correo    VARCHAR(80) NOT NULL,
  telefono  VARCHAR(20) NOT NULL,

  CONSTRAINT uq_clientes_documento UNIQUE (documento)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- PRESTAMOS
--
-- Cambios frente al script guia (permitidos por el enunciado):
--   * tipo_interes: guarda la estrategia de calculo usada (SIMPLE, COMPUESTO o
--     CUOTA_FIJA). Sin esta columna, al releer un prestamo desde la base no se
--     podria reconstruir su monto total ni su cuota.
--   * ON DELETE RESTRICT: un cliente o un empleado con prestamos no se puede
--     borrar; la cartera es informacion contable.
--   * Indices sobre las llaves foraneas y sobre estado, que son las columnas por
--     las que el modulo de reportes filtra.
-- ----------------------------------------------------------------------------
CREATE TABLE prestamos (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  cliente_id   INT           NOT NULL,
  empleado_id  INT           NOT NULL,
  monto        DECIMAL(12,2) NOT NULL,
  interes      DECIMAL(5,2)  NOT NULL,
  cuotas       INT           NOT NULL,
  fecha_inicio DATE          NOT NULL,
  estado       VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
  tipo_interes VARCHAR(20)   NOT NULL DEFAULT 'SIMPLE',

  CONSTRAINT fk_prestamos_cliente  FOREIGN KEY (cliente_id)  REFERENCES clientes(id)  ON DELETE RESTRICT,
  CONSTRAINT fk_prestamos_empleado FOREIGN KEY (empleado_id) REFERENCES empleados(id) ON DELETE RESTRICT,
  CONSTRAINT ck_prestamos_monto    CHECK (monto > 0),
  CONSTRAINT ck_prestamos_interes  CHECK (interes >= 0 AND interes <= 100),
  CONSTRAINT ck_prestamos_cuotas   CHECK (cuotas BETWEEN 1 AND 120),
  CONSTRAINT ck_prestamos_estado   CHECK (estado IN ('PENDIENTE','PAGADO')),
  CONSTRAINT ck_prestamos_tipo     CHECK (tipo_interes IN ('SIMPLE','COMPUESTO','CUOTA_FIJA'))
) ENGINE = InnoDB;

CREATE INDEX idx_prestamos_cliente  ON prestamos (cliente_id);
CREATE INDEX idx_prestamos_empleado ON prestamos (empleado_id);
CREATE INDEX idx_prestamos_estado   ON prestamos (estado);

-- ----------------------------------------------------------------------------
-- PAGOS
-- ON DELETE CASCADE: los abonos no tienen sentido sin su prestamo.
-- ----------------------------------------------------------------------------
CREATE TABLE pagos (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  prestamo_id INT           NOT NULL,
  fecha_pago  DATE          NOT NULL,
  monto       DECIMAL(12,2) NOT NULL,

  CONSTRAINT fk_pagos_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos(id) ON DELETE CASCADE,
  CONSTRAINT ck_pagos_monto    CHECK (monto > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_pagos_prestamo ON pagos (prestamo_id);

-- ----------------------------------------------------------------------------
-- VISTA DE SALDOS
-- La aplicacion calcula el saldo en Java con Stream API (es parte del reto),
-- pero dejar la vista permite verificar los mismos numeros desde SQL durante la
-- sustentacion.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_saldo_prestamos AS
SELECT  p.id                                   AS prestamo_id,
        c.nombre                               AS cliente,
        p.monto                                AS capital,
        p.estado                               AS estado,
        DATE_ADD(p.fecha_inicio, INTERVAL p.cuotas MONTH) AS fecha_vencimiento,
        COALESCE(SUM(pg.monto), 0)             AS total_abonado
FROM        prestamos p
JOIN        clientes  c  ON c.id = p.cliente_id
LEFT JOIN   pagos     pg ON pg.prestamo_id = p.id
GROUP BY    p.id, c.nombre, p.monto, p.estado, p.fecha_inicio, p.cuotas;

-- ============================================================================
--  DATOS DE PRUEBA
--  Las fechas son relativas a CURDATE() para que siempre existan prestamos
--  vigentes y prestamos vencidos sin importar cuando se ejecute el script.
-- ============================================================================

INSERT INTO empleados (nombre, documento, rol, correo, salario) VALUES
  ('Ana Maria Gomez',   '1098765432', 'ASESOR',        'ana.gomez@crediya.co',   3200000.00),
  ('Carlos Pena Ruiz',  '91234567',   'COBRADOR',      'carlos.pena@crediya.co', 2400000.00),
  ('Laura Rincon',      '1020304050', 'ANALISTA',      'laura.rincon@crediya.co',3800000.00),
  ('Jorge Villamizar',  '79856231',   'ADMINISTRADOR', 'jorge.v@crediya.co',     6500000.00);

INSERT INTO clientes (nombre, documento, correo, telefono) VALUES
  ('Pedro Sanchez Diaz', '1005432198', 'pedro.sanchez@correo.com', '3105558877'),
  ('Marta Quintero',     '63301122',   'marta.q@correo.com',       '3009991122'),
  ('Luis Fernando Ariza','1090443322', 'lf.ariza@correo.com',      '3187774455'),
  ('Sofia Carreno',      '1101223344', 'sofia.carreno@correo.com', '3024446677'),
  ('Diego Mantilla',     '88112233',   'diego.mantilla@correo.com','3156663322');

INSERT INTO prestamos (cliente_id, empleado_id, monto, interes, cuotas, fecha_inicio, estado, tipo_interes) VALUES
  (1, 1,  5000000.00, 2.00, 12, DATE_SUB(CURDATE(), INTERVAL  3 MONTH), 'PENDIENTE', 'SIMPLE'),
  (2, 1,  2000000.00, 2.50,  6, DATE_SUB(CURDATE(), INTERVAL  9 MONTH), 'PENDIENTE', 'SIMPLE'),
  (3, 2,  8000000.00, 1.80, 24, DATE_SUB(CURDATE(), INTERVAL  2 MONTH), 'PENDIENTE', 'CUOTA_FIJA'),
  (4, 3,  1500000.00, 3.00,  4, DATE_SUB(CURDATE(), INTERVAL 10 MONTH), 'PENDIENTE', 'COMPUESTO'),
  (5, 2,  3000000.00, 2.20,  6, DATE_SUB(CURDATE(), INTERVAL  8 MONTH), 'PAGADO',    'SIMPLE'),
  (1, 3, 12000000.00, 1.50, 36, DATE_SUB(CURDATE(), INTERVAL  1 MONTH), 'PENDIENTE', 'CUOTA_FIJA');

INSERT INTO pagos (prestamo_id, fecha_pago, monto) VALUES
  (1, DATE_SUB(CURDATE(), INTERVAL 2 MONTH),  516666.67),
  (1, DATE_SUB(CURDATE(), INTERVAL 1 MONTH),  516666.67),
  (2, DATE_SUB(CURDATE(), INTERVAL 8 MONTH),  383333.33),
  (3, DATE_SUB(CURDATE(), INTERVAL 1 MONTH),  412000.00),
  (4, DATE_SUB(CURDATE(), INTERVAL 9 MONTH),  200000.00),
  (5, DATE_SUB(CURDATE(), INTERVAL 7 MONTH), 1700000.00),
  (5, DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 1696000.00),
  (6, DATE_SUB(CURDATE(), INTERVAL 1 MONTH),  420000.00);

SELECT 'Base de datos crediya_db creada correctamente.' AS resultado;
