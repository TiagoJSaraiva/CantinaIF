#!/bin/sh
# Executa o docker-entrypoint.sh DE VERDADE contra uma copia isolada de um Tomcat 10.1 e confere o resultado.
# Nao exige Docker. Requisitos: sh, curl, awk, sed, JDK 21 (JAVA_HOME) e um Tomcat 10.1 descompactado.
#
# uso: TOMCAT_HOME=/caminho/apache-tomcat-10.1.x JAVA_HOME=... tools/test-entrypoint.sh [arquivo.war]
set -eu
: "${TOMCAT_HOME:?defina TOMCAT_HOME}"
: "${JAVA_HOME:?defina JAVA_HOME}"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
WAR="${1:-$(ls "$RAIZ"/target/*.war | head -1)}"
PORTA_TRUSTED="${PORTA_TRUSTED:-18091}"
PORTA_UNTRUSTED="${PORTA_UNTRUSTED:-18092}"
TMP="$(mktemp -d)"
PID=""

parar() {
  if [ -n "$PID" ]; then kill "$PID" 2>/dev/null || true; sleep 3; PID=""; fi
}
limpar() { parar; rm -rf "$TMP"; }
trap limpar EXIT

iniciar() { # nome porta behind_proxy [proxies]
  BASE="$TMP/$1"
  mkdir -p "$BASE/conf" "$BASE/webapps" "$BASE/logs" "$BASE/temp" "$BASE/work"
  for f in server.xml web.xml catalina.properties logging.properties context.xml; do cp "$TOMCAT_HOME/conf/$f" "$BASE/conf/"; done
  sed -i 's/port="8005"/port="-1"/' "$BASE/conf/server.xml"   # sem porta de shutdown (evita conflito)
  cp "$WAR" "$BASE/webapps/ROOT.war"
  (
    export CATALINA_HOME="$TOMCAT_HOME" CATALINA_BASE="$BASE" CATALINA_TMPDIR="$BASE/temp"
    export PORT="$2" JAVA_OPTS="-DCANTINA_DEMO_RESET_HOURS=0"
    if [ "$3" = "true" ]; then export CANTINA_BEHIND_PROXY=true; else unset CANTINA_BEHIND_PROXY; fi
    [ -n "${4:-}" ] && export CANTINA_TRUSTED_PROXIES="$4"
    PATH="$TOMCAT_HOME/bin:$PATH"
    exec sh "$RAIZ/docker-entrypoint.sh"
  ) > "$BASE/saida.log" 2>&1 &
  PID=$!
  for _ in $(seq 1 60); do
    sleep 2
    [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$2/health" || true)" = "200" ] && return 0
  done
  echo "Tomcat nao subiu na porta $2; ultimas linhas do log:"; tail -30 "$BASE/saida.log"; return 1
}

echo "##### 1) CANTINA_BEHIND_PROXY=true (regex padrao) na porta $PORTA_TRUSTED"
iniciar trusted "$PORTA_TRUSTED" true
grep -c 'RemoteIpValve' "$TMP/trusted/conf/server.xml" | grep -qx 1 && echo "  ok   - RemoteIpValve inserido uma unica vez"
grep -q 'ErrorReportValve' "$TMP/trusted/conf/server.xml" && echo "  ok   - ErrorReportValve inserido"
"$RAIZ/tools/smoke-proxy.sh" "http://127.0.0.1:$PORTA_TRUSTED" trusted
parar

echo "##### 2) sem CANTINA_BEHIND_PROXY na porta $PORTA_UNTRUSTED"
iniciar untrusted "$PORTA_UNTRUSTED" false
grep -q 'RemoteIpValve' "$TMP/untrusted/conf/server.xml" && { echo "  FALHA - RemoteIpValve nao deveria existir"; exit 1; } || echo "  ok   - sem RemoteIpValve"
"$RAIZ/tools/smoke-proxy.sh" "http://127.0.0.1:$PORTA_UNTRUSTED" untrusted
parar

echo "##### 3) idempotencia: executar de novo sobre o mesmo server.xml nao duplica as valvulas"
iniciar trusted "$PORTA_TRUSTED" true
parar
CONF="$TMP/trusted/conf/server.xml"
( export CATALINA_HOME="$TOMCAT_HOME" CATALINA_BASE="$TMP/trusted" PORT="$PORTA_TRUSTED" CANTINA_BEHIND_PROXY=true
  PATH="$TMP:$PATH"; printf '#!/bin/sh\nexit 0\n' > "$TMP/catalina.sh"; chmod +x "$TMP/catalina.sh"
  sh "$RAIZ/docker-entrypoint.sh" )
[ "$(grep -c 'RemoteIpValve' "$CONF")" = "1" ] && [ "$(grep -c 'ErrorReportValve' "$CONF")" = "1" ] \
  && echo "  ok   - valvulas nao duplicadas" || { echo "  FALHA - valvulas duplicadas"; exit 1; }
echo "TUDO OK"
