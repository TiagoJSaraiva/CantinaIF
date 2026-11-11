#!/bin/sh
# Inicia o Tomcat na porta indicada por PORT (padrao 8080), compativel com plataformas que injetam PORT.
#
# Atras de um proxy HTTPS (ex.: Render): defina CANTINA_BEHIND_PROXY=true. O RemoteIpValve do server.xml
# passa a confiar em X-Forwarded-For / X-Forwarded-Proto SOMENTE quando a conexao vem de um proxy confiavel
# (enderecos privados por padrao; sobrescreva com CANTINA_TRUSTED_PROXIES, uma regex Java).
# Sem a variavel, a regex e "a^" (nunca casa) e os cabecalhos X-Forwarded-* sao ignorados.
set -e

DEFAULT_PROXIES='10\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+|172\.(1[6-9]|2[0-9]|3[01])\.\d+\.\d+|127\.\d+\.\d+\.\d+|100\.(6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\.\d+\.\d+|0:0:0:0:0:0:0:1|::1'

if [ "$CANTINA_BEHIND_PROXY" = "true" ]; then
  PROXIES="${CANTINA_TRUSTED_PROXIES:-$DEFAULT_PROXIES}"
else
  PROXIES='a^'
fi

export JAVA_OPTS="$JAVA_OPTS -Dhttp.port=${PORT:-8080} -Dcantina.trusted.proxies=$PROXIES"
exec catalina.sh run
