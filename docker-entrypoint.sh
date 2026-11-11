#!/bin/sh
# Inicia o Tomcat na porta indicada por PORT (padrao 8080), compativel com plataformas que injetam PORT.
#
# Configuracao do server.xml feita aqui, de forma idempotente (nada de regex em JAVA_OPTS: o catalina.sh
# faz eval e quebraria com "|" e "("):
#   - porta HTTP vinda de ${http.port} (definida a partir de PORT);
#   - ErrorReportValve sem relatorio/versao do servidor (sem stack trace nem "Apache Tomcat/x.y");
#   - RemoteIpValve SOMENTE com CANTINA_BEHIND_PROXY=true: confia em X-Forwarded-For/X-Forwarded-Proto
#     apenas quando a conexao vem de um proxy confiavel (enderecos privados por padrao; sobrescreva com
#     CANTINA_TRUSTED_PROXIES, uma regex Java).
set -e

BASE="${CATALINA_BASE:-${CATALINA_HOME:-/usr/local/tomcat}}"
CONF="$BASE/conf/server.xml"
DEFAULT_PROXIES='10\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+|172\.(1[6-9]|2[0-9]|3[01])\.\d+\.\d+|127\.\d+\.\d+\.\d+|100\.(6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\.\d+\.\d+|0:0:0:0:0:0:0:1|::1'

# 1) porta
sed -i 's/port="8080"/port="${http.port:-8080}"/' "$CONF"

# 1b) conector HTTP: limites de corpo/cabecalho/parametros e tempos limite contra clientes lentos
#     (substitui o primeiro <Connector> ativo; idempotente por marcadores)
CONN="$(mktemp)"
cat > "$CONN" <<'XML'
    <Connector port="${http.port:-8080}" protocol="HTTP/1.1"
               redirectPort="8443"
               connectionTimeout="10000"
               connectionUploadTimeout="5000"
               disableUploadTimeout="false"
               keepAliveTimeout="10000"
               maxKeepAliveRequests="100"
               maxThreads="60"
               acceptCount="50"
               maxHttpHeaderSize="8192"
               maxPostSize="16384"
               maxSwallowSize="65536"
               maxParameterCount="60"
               />
XML
TMPC="$(mktemp)"
awk -v cf="$CONN" '
  /CANTINA_CONNECTOR_BEGIN/ { print; while ((getline l < cf) > 0) print l; skip = 1; feito = 1; next }
  /CANTINA_CONNECTOR_END/   { skip = 0 }
  skip == 1                 { next }
  !feito && /<Connector port="(8080|\$\{http\.port)/ {
    print "<!-- CANTINA_CONNECTOR_BEGIN -->"
    while ((getline l < cf) > 0) print l
    print "<!-- CANTINA_CONNECTOR_END -->"
    feito = 1
    emconector = ($0 ~ /\/>/) ? 0 : 1
    next
  }
  emconector { if ($0 ~ /\/>/) emconector = 0; next }
  { print }
' "$CONF" > "$TMPC"
cat "$TMPC" > "$CONF"
rm -f "$CONN" "$TMPC"

# 2) marcadores (inseridos uma unica vez, antes do </Host>)
if ! grep -q 'CANTINA_VALVES_BEGIN' "$CONF"; then
  sed -i 's#</Host>#<!-- CANTINA_VALVES_BEGIN -->\n<!-- CANTINA_VALVES_END -->\n</Host>#' "$CONF"
fi

# 3) valvulas geradas num arquivo temporario e copiadas com awk (sem interpretar caracteres especiais)
VALVES="$(mktemp)"
{
  echo '<Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false" />'
  if [ "$CANTINA_BEHIND_PROXY" = "true" ]; then
    PROXIES="${CANTINA_TRUSTED_PROXIES:-$DEFAULT_PROXIES}"
    ESC=$(printf '%s' "$PROXIES" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' -e 's/"/\&quot;/g')
    printf '<Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="X-Forwarded-For" protocolHeader="X-Forwarded-Proto" internalProxies="%s" />\n' "$ESC"
  fi
} > "$VALVES"

TMP="$(mktemp)"
awk -v vf="$VALVES" '
  /CANTINA_VALVES_BEGIN/ { print; while ((getline linha < vf) > 0) print linha; skip = 1; next }
  /CANTINA_VALVES_END/   { skip = 0 }
  skip == 0              { print }
' "$CONF" > "$TMP"
cat "$TMP" > "$CONF"
rm -f "$VALVES" "$TMP"

export JAVA_OPTS="$JAVA_OPTS -Dhttp.port=${PORT:-8080}"
exec catalina.sh run
