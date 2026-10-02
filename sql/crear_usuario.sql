-- ============================================================================
--  Usuario de aplicacion para CrediYa
--
--  La aplicacion NUNCA debe conectarse como root. Este script crea un usuario
--  con permisos limitados unicamente a crediya_db.
--
--  1. Reemplace CAMBIE_ESTA_CLAVE por una clave propia.
--  2. Ejecute:  mysql -u root -p < sql/crear_usuario.sql
--  3. Ponga la misma clave en crediya.local.properties o en la variable de
--     entorno CREDIYA_DB_PASSWORD.
--
--  Este archivo se versiona SOLO como plantilla: no debe contener la clave real.
-- ============================================================================

CREATE USER IF NOT EXISTS 'crediya_app'@'localhost'
  IDENTIFIED BY 'CAMBIE_ESTA_CLAVE';

GRANT SELECT, INSERT, UPDATE, DELETE ON crediya_db.* TO 'crediya_app'@'localhost';

FLUSH PRIVILEGES;

SELECT 'Usuario crediya_app creado. Recuerde cambiar la clave de la plantilla.' AS resultado;
