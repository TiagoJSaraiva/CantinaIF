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
printf '%s\n' "$CABECALHOS" | grep -qi 'JSESSIONID' && erro "visitante anonimo nao deve ganhar sessao" || ok "login.jsp nao cria sessao para anonimo"
COOKIE=$(printf '%s\n' "$CABECALHOS" | grep -i '^set-cookie: csrf_anon')
[ -n "$COOKIE" ] && ok "cookie CSRF anonimo emitido" || erro "login.jsp deveria emitir o cookie csrf_anon"
if [ "$MODO" = "trusted" ]; then
  printf '%s' "$COOKIE" | grep -qi 'Secure' && ok "cookie Secure (esquema https resolvido pelo RemoteIpValve)" || erro "cookie deveria ser Secure"
  printf '%s\n' "$CABECALHOS" | grep -qi '^strict-transport-security' && ok "HSTS presente" || erro "HSTS deveria estar presente"
else
  printf '%s' "$COOKIE" | grep -qi 'Secure' && erro "cabecalho X-Forwarded-Proto deveria ser ignorado (cookie nao deve ser Secure)" || ok "X-Forwarded-Proto ignorado"
  printf '%s\n' "$CABECALHOS" | grep -qi '^strict-transport-security' && erro "HSTS nao deveria estar presente" || ok "sem HSTS"
fi
printf '%s' "$COOKIE" | grep -qi 'HttpOnly' && ok "cookie HttpOnly" || erro "cookie deveria ser HttpOnly"
printf '%s' "$COOKIE" | grep -qi 'SameSite=Strict' && ok "cookie SameSite=Strict" || erro "cookie deveria ter SameSite=Strict"

if [ "$MODO" = "trusted" ]; then
  echo "== limite de login usa o IP real do cliente (X-Forwarded-For) quando o proxy e confiavel"
  H1='X-Forwarded-Proto: https'
  curl -s -D hs.$$.txt -o hs.$$.html -H "$H1" "$BASE/login.jsp"
  ANON=$(grep -io 'csrf_anon=[^;]*' hs.$$.txt | head -1)
  TOK=$(grep -o 'name="_csrf" value="[^"]*"' hs.$$.html | head -1 | cut -d'"' -f4)
  rm -f hs.$$.txt hs.$$.html
  i=1
  while [ $i -le 41 ]; do
    curl -s -o /dev/null -H "$H1" -H 'X-Forwarded-For: 203.0.113.50' -H "Cookie: $ANON" \
      -d "_csrf=$TOK&prontuario=XX$i&senha=errada$i" "$BASE/login"
    i=$((i + 1))
  done
  A=$(curl -s -o /dev/null -w '%{http_code}' -H "$H1" -H 'X-Forwarded-For: 203.0.113.50' -H "Cookie: $ANON" -d "_csrf=$TOK&prontuario=YY&senha=x" "$BASE/login")
  B=$(curl -s -o /dev/null -w '%{http_code}' -H "$H1" -H 'X-Forwarded-For: 203.0.113.51' -H "Cookie: $ANON" -d "_csrf=$TOK&prontuario=YY&senha=x" "$BASE/login")
  [ "$A" = "429" ] && ok "IP 203.0.113.50 bloqueado apos 40 falhas (429)" || erro "IP do cliente deveria estar bloqueado (recebido $A)"
  [ "$B" = "200" ] && ok "outro IP (203.0.113.51) livre (200)" || erro "outro IP nao deveria estar bloqueado (recebido $B)"
fi

echo "== corpo de requisicao enorme e recusado (413) sem derrubar o servidor"
CORPO413=$(head -c 40000 /dev/zero | curl -s -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' --data-binary @- "$BASE/carrinho")
[ "$CORPO413" = "413" ] && ok "POST de 40 KB em /carrinho: 413" || erro "esperado 413, recebido $CORPO413"
[ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/health")" = "200" ] && ok "servidor continua saudavel" || erro "/health falhou apos o corpo grande"

echo "== pagina de erro sem versao do servidor nem stack trace"
CORPO=$(curl -s "$BASE/rota-que-nao-existe")
printf '%s' "$CORPO" | grep -qi 'Apache Tomcat\|at org.apache' && erro "pagina de erro vaza informacao do servidor" || ok "sem vazamento"

if [ "$FALHAS" -eq 0 ]; then echo "OK"; else echo "$FALHAS falha(s)"; exit 1; fi
