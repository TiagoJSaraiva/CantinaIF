<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="compradores"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Compradores - Painel do vendedor</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo">
    <div class="page-head"><h1>Compradores</h1></div>

    <section class="painel painel-codigo" aria-labelledby="titulo-codigo">
        <div>
            <h2 id="titulo-codigo">Código de vínculo</h2>
            <p class="dica">Gere um código e mostre ao aluno: ele digita o código no cadastro ou em "Minha conta" e passa a
                ver os seus produtos. Cada código vale por <strong>1 minuto</strong> e para <strong>um único aluno</strong>.</p>
        </div>
        <div class="codigo-area">
            <output id="codigo" class="codigo-grande" aria-live="polite" hidden></output>
            <p id="codigo-tempo" class="codigo-tempo" hidden></p>
            <button type="button" class="btn" id="btn-gerar">Gerar código</button>
        </div>
    </section>

    <section aria-labelledby="titulo-lista">
        <div class="page-head">
            <h2 id="titulo-lista">Seus compradores</h2>
            <button type="button" class="btn btn-secundario btn-pequeno" id="btn-atualizar">Atualizar</button>
        </div>
        <div class="toolbar">
            <div class="busca">
                <label for="busca" class="sr-only">Buscar comprador</label>
                <input type="search" id="busca" placeholder="Buscar por nome, prontuário ou e-mail..." autocomplete="off">
            </div>
            <div class="chips" role="group" aria-label="Filtrar por situação" id="filtros-situacao">
                <button type="button" class="chip" data-situacao="todos" aria-pressed="true">Todos</button>
                <button type="button" class="chip" data-situacao="ativos" aria-pressed="false">Ativos</button>
                <button type="button" class="chip" data-situacao="desativados" aria-pressed="false">Desativados</button>
            </div>
        </div>
        <div id="estado" class="estado carregando" role="status">Carregando compradores...</div>
        <div class="tabela-rolagem" id="area-tabela" hidden>
            <table class="tabela tabela-compradores">
                <caption class="sr-only">Compradores vinculados à sua cantina</caption>
                <thead>
                    <tr>
                        <th scope="col">Comprador</th>
                        <th scope="col" class="num">Pedidos</th>
                        <th scope="col" class="num" title="Pedidos que o próprio aluno cancelou">Cancelou</th>
                        <th scope="col" class="num" title="Pedidos que você cancelou">Cancelados por você</th>
                        <th scope="col">Último pedido</th>
                        <th scope="col" class="num">Advertências</th>
                        <th scope="col">Situação</th>
                        <th scope="col"><span class="sr-only">Ações</span></th>
                    </tr>
                </thead>
                <tbody id="linhas"></tbody>
            </table>
        </div>
    </section>
</main>

<dialog id="dlg-advertencia" aria-labelledby="dlg-adv-titulo">
    <h2 id="dlg-adv-titulo">Enviar advertência</h2>
    <form id="form-advertencia" novalidate data-validar>
        <p class="dica" id="dlg-adv-para"></p>
        <fieldset class="modelos">
            <legend>Mensagens prontas</legend>
            <button type="button" class="chip js-modelo">Cancelamentos estão ocorrendo com frequência. Se continuar, você poderá ser multado.</button>
            <button type="button" class="chip js-modelo">Você não retirou pedidos que ficaram prontos. Retire no horário ou sua conta poderá ser desativada.</button>
            <button type="button" class="chip js-modelo">Comportamento inadequado no balcão. Na próxima ocorrência sua conta será desativada.</button>
        </fieldset>
        <div class="field">
            <label for="adv-mensagem">Mensagem</label>
            <textarea id="adv-mensagem" name="mensagem" required minlength="3" maxlength="500"></textarea>
            <span class="dica"><span id="adv-contador">0</span>/500 caracteres. O aluno vê a mensagem ao entrar.</span>
            <span class="campo-erro" id="erro-mensagem"></span>
        </div>
        <div class="dialogo-acoes">
            <button type="button" class="btn btn-secundario" id="btn-adv-cancelar">Cancelar</button>
            <button type="submit" class="btn" id="btn-adv-enviar">Enviar advertência</button>
        </div>
    </form>
</dialog>
<script src="js/util.js"></script>
<script src="js/compradores.js"></script>
</body>
</html>
