#!/usr/bin/env bash
# ----------------------------------------------------------------------------
#  CrediYa - compila y ejecuta el sistema.
#
#  Uso:
#    ./run.sh              almacen segun crediya.properties (MySQL por defecto)
#    ./run.sh archivo      fuerza los archivos de texto de datos/
#    ./run.sh memoria      fuerza el almacen en memoria (demostracion rapida)
#    ./run.sh mysql        fuerza MySQL
# ----------------------------------------------------------------------------
set -e
cd "$(dirname "$0")"

if [ -n "$1" ]; then
  export CREDIYA_PERSISTENCIA="$1"
  echo "Almacen forzado por linea de comandos: $CREDIYA_PERSISTENCIA"
fi

mvn -q -B compile
mvn -q -B exec:java -Dexec.cleanupDaemonThreads=false
