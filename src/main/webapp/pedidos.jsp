<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="pedidos"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Pedidos - Painel do vendedor</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo">
    <div class="page-head">
        <h1>Pedidos</h1>
        <button type="button" class="btn btn-secundario" id="btn-atualizar">Atualizar</button>
    </div>

    <div class="toolbar">
        <div class="chips" role="group" aria-label="Filtrar por status" id="filtros-status"></div>
    </div>

    <div id="mensagem" role="status" aria-live="polite" hidden></div>
    <div id="estado" class="estado carregando" role="status">Carregando pedidos...</div>
    <div id="lista" class="lista-pedidos" hidden></div>
</main>

<dialog id="dlg-cancelar" aria-labelledby="dlg-cancelar-titulo">
    <h2 id="dlg-cancelar-titulo">Cancelar pedido</h2>
    <form id="form-cancelar" novalidate data-validar>
        <p class="dica" id="dlg-cancelar-info"></p>
        <fieldset class="modelos">
            <legend>Motivos comuns</legend>
            <button type="button" class="chip js-motivo">Um dos produtos acabou.</button>
            <button type="button" class="chip js-motivo">A cantina vai fechar antes de o pedido ficar pronto.</button>
            <button type="button" class="chip js-motivo">O pedido não foi retirado no horário.</button>
        </fieldset>
        <div class="field">
            <label for="motivo">Motivo do cancelamento</label>
            <textarea id="motivo" name="motivo" required minlength="3" maxlength="300"></textarea>
            <span class="dica">Obrigatório. O aluno vê este motivo em "Meus pedidos". O estoque dos itens é devolvido.</span>
            <span class="campo-erro" id="erro-motivo"></span>
        </div>
        <div class="dialogo-acoes">
            <button type="button" class="btn btn-secundario" id="btn-cancelar-voltar">Voltar</button>
            <button type="submit" class="btn btn-perigo" id="btn-cancelar-confirmar">Cancelar pedido</button>
        </div>
    </form>
</dialog>
<script src="js/util.js"></script>
<script src="js/pedidos.js"></script>
</body>
</html>
