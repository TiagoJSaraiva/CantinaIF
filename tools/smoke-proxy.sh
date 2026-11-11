#!/bin/sh
# Verificacoes de fumaca de uma instancia ja em execucao.
# uso: tools/smoke-proxy.sh <url-base sem barra final> <trusted|untrusted>
#   trusted   = a instancia foi iniciada com CANTINA_BEHIND_PROXY=true e o cliente (este script) e um proxy confiavel
#   untrusted = CANTINA_BEHIND_PROXY nao definida: os cabecalhos X-Forwarded-* devem ser ignorados
set -u
BASE="${1:?url base}"
MODO="${2:?trusted|untrusted}"
FALHAS=0

ok()   { echo "  ok   - $1"; }
erro() { echo "  FALHA - $1"; FALHAS=$((FALHAS + 1)); }

echo "== /health (publico, sem login, sem cookie)"
CODIGO=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/health")
[ "$CODIGO" = "200" ] && ok "HTTP 200" || erro "esperado 200, recebido $CODIGO"
curl -s -D - -o /dev/null "$BASE/health" | grep -qi '^set-cookie' && erro "/health nao deve criar sessao" || ok "sem cookie de sessao"

echo "== atras do proxy (X-Forwarded-Proto: https, X-Forwarded-For: 203.0.113.7), modo $MODO"
CABECALHOS=$(curl -s -D - -o /dev/null -H 'X-Forwarded-Proto: https' -H 'X-Forwarded-For: 203.0.113.7' "$BASE/login.jsp")
COOKIE=$(printf '%s\n' "$CABECALHOS" | grep -i '^set-cookie: JSESSIONID')
[ -n "$COOKIE" ] && ok "cookie de sessao emitido" || erro "login.jsp deveria emitir cookie de sessao"
if [ "$MODO" = "trusted" ]; then
  printf '%s' "$COOKIE" | grep -qi 'Secure' && ok "cookie Secure (esquema https resolvido pelo RemoteIpValve)" || erro "cookie deveria ser Secure"
  printf '%s\n' "$CABECALHOS" | grep -qi '^strict-transport-security' && ok "HSTS presente" || erro "HSTS deveria estar presente"
else
  printf '%s' "$COOKIE" | grep -qi 'Secure' && erro "cabecalho X-Forwarded-Proto deveria ser ignorado (cookie nao deve ser Secure)" || ok "X-Forwarded-Proto ignorado"
  printf '%s\n' "$CABECALHOS" | grep -qi '^strict-transport-security' && erro "HSTS nao deveria estar presente" || ok "sem HSTS"
fi
printf '%s' "$COOKIE" | grep -qi 'HttpOnly' && ok "cookie HttpOnly" || erro "cookie deveria ser HttpOnly"
printf '%s' "$COOKIE" | grep -qi 'SameSite=Lax' && ok "cookie SameSite=Lax" || erro "cookie deveria ter SameSite=Lax"

echo "== pagina de erro sem versao do servidor nem stack trace"
CORPO=$(curl -s "$BASE/rota-que-nao-existe")
printf '%s' "$CORPO" | grep -qi 'Apache Tomcat\|at org.apache' && erro "pagina de erro vaza informacao do servidor" || ok "sem vazamento"

if [ "$FALHAS" -eq 0 ]; then echo "OK"; else echo "$FALHAS falha(s)"; exit 1; fi
