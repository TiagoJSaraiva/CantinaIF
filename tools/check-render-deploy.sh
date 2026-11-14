#!/bin/sh
# Espera o servico publicado servir um commit especifico, lido do cabecalho X-App-Commit de /health.
#   uso: sh tools/check-render-deploy.sh <url-base> <sha-esperado> [limite-em-segundos]
# Sai com 0 quando o commit esperado esta no ar e com 1 se o limite (padrao 900 s) acabar antes.
# Feito para o CI: se o push nao gerar deploy (ex.: app do Render sem acesso ao repositorio) ou o deploy
# falhar (o Render mantem a versao anterior no ar), o job fica vermelho em vez de a falha passar despercebida.
set -u

BASE="${1:?informe a URL base, ex.: https://cantinaif-ubt0.onrender.com}"
ESPERADO="$(printf '%s' "${2:?informe o SHA esperado}" | tr 'A-F' 'a-f')"
LIMITE="${3:-900}"
INTERVALO=20

inicio=$(date +%s)
anterior=""
while :; do
  # --max-time alto: a instancia gratuita hiberna e leva cerca de 1 minuto para acordar
  atual=$(curl -s -o /dev/null -D - --max-time 90 "$BASE/health" 2>/dev/null \
    | tr -d '\r' | awk -F': *' 'tolower($1) == "x-app-commit" { print tolower($2) }' | tail -n 1)
  [ -n "$atual" ] || atual="(sem resposta ou sem cabecalho)"

  case "$atual" in
    [0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]*)
      case "$ESPERADO" in
        "$atual"*) echo "OK: $BASE serve o commit $ESPERADO"; exit 0 ;;
      esac
      case "$atual" in
        "$ESPERADO"*) echo "OK: $BASE serve o commit $ESPERADO"; exit 0 ;;
      esac
      ;;
  esac

  decorrido=$(( $(date +%s) - inicio ))
  if [ "$atual" != "$anterior" ]; then
    echo "[$decorrido s] no ar: $atual; aguardando $ESPERADO"
    anterior="$atual"
  fi
  if [ "$decorrido" -ge "$LIMITE" ]; then
    echo "ERRO: depois de $LIMITE s, $BASE ainda serve: $atual (esperado $ESPERADO)." >&2
    echo "Veja a aba Events do servico no painel do Render:" >&2
    echo "  - nenhum deploy para este commit: o app do Render no GitHub perdeu acesso ao repositorio" >&2
    echo "    (github.com/settings/installations) ou o Auto-Deploy foi desligado;" >&2
    echo "  - deploy com falha: o log do build/inicio mostra a causa (o Render mantem a versao anterior no ar);" >&2
    echo "  - 'desconhecido': a imagem no ar nao informa o commit (RENDER_GIT_COMMIT ausente)." >&2
    exit 1
  fi
  sleep "$INTERVALO"
done
