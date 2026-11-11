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
<script src="js/util.js"></script>
<script src="js/pedidos.js"></script>
</body>
</html>
