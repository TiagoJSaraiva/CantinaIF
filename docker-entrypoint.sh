#!/bin/sh
# Inicia o Tomcat na porta indicada por PORT (padrao 8080), compativel com plataformas que injetam PORT.
set -e
export JAVA_OPTS="$JAVA_OPTS -Dhttp.port=${PORT:-8080}"
exec catalina.sh run
