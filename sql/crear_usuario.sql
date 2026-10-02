-- ============================================================================
--  Usuario de aplicacion para CrediYa
--
--  !! NO EJECUTE ESTE ARCHIVO TAL CUAL !!
--
--  CAMBIE_ESTA_CLAVE es un marcador de posicion, no una clave. Si ejecuta el
--  script sin reemplazarlo, la cuenta de la aplicacion queda con una clave que
--  esta publicada en este mismo repositorio.
--
--  Forma recomendada (no modifica este archivo, que esta versionado):
--
--      printf 'Clave para crediya_app: '; stty -echo; read CLAVE; stty echo; echo
--      sed "s/CAMBIE_ESTA_CLAVE/$CLAVE/" sql/crear_usuario.sql | mysql -u root -p
--      printf 'crediya.db.clave=%s\n' "$CLAVE" > crediya.local.properties
--      unset CLAVE          # (sirve igual en bash y en zsh)
--
--  La aplicacion nunca se conecta como root: esta cuenta solo puede leer y
--  escribir en crediya_db, y no puede crear ni borrar tablas.
--
--  El script es idempotente: volver a ejecutarlo con otra clave la actualiza.
-- ============================================================================

-- CREATE ... IF NOT EXISTS no toca la clave de una cuenta que ya exista, asi que
-- el ALTER de la linea siguiente es el que realmente la fija. Con los dos, el
-- script funciona igual si la cuenta es nueva o si ya estaba creada.
CREATE USER IF NOT EXISTS 'crediya_app'@'localhost'
  IDENTIFIED BY 'CAMBIE_ESTA_CLAVE';

ALTER USER 'crediya_app'@'localhost'
  IDENTIFIED BY 'CAMBIE_ESTA_CLAVE';

GRANT SELECT, INSERT, UPDATE, DELETE ON crediya_db.* TO 'crediya_app'@'localhost';

FLUSH PRIVILEGES;

SELECT 'Usuario crediya_app listo.' AS resultado;
