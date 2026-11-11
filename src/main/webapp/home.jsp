<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="home"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Vitrine - CantinaIF</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo">
    <div class="page-head"><h1>Vitrine</h1></div>

    <div class="toolbar">
        <div class="busca">
            <label for="busca" class="sr-only">Buscar produtos</label>
            <input type="search" id="busca" placeholder="Buscar produto..." autocomplete="off">
        </div>
        <div class="chips" role="group" aria-label="Filtrar por categoria">
            <button type="button" class="chip" data-filter="todos" aria-pressed="true">Todos</button>
            <button type="button" class="chip" data-filter="salgado" aria-pressed="false">Salgados</button>
            <button type="button" class="chip" data-filter="lanche" aria-pressed="false">Lanches</button>
            <button type="button" class="chip" data-filter="doce" aria-pressed="false">Doces</button>
            <button type="button" class="chip" data-filter="bebida" aria-pressed="false">Bebidas</button>
        </div>
    </div>

    <div id="mensagem" role="status" aria-live="polite" hidden></div>
    <div id="estado" class="estado carregando" role="status">Carregando produtos...</div>
    <div id="productContainer" class="grade" hidden></div>
</main>
<script src="js/util.js"></script>
<script src="js/filtro.js"></script>
</body>
</html>
