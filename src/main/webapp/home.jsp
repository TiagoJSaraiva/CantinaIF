<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="cif" %>
<c:set var="pagina" value="home"/>
<c:set var="estado" value="${requestScope.estadoComprador}"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Catálogo - CantinaIF</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo">
    <div class="notificacoes" id="notificacoes" aria-live="polite">
        <c:if test="${estado.vinculado and estado.naoLidas > 0}">
            <cif:notificacao tipo="aviso" chave="adv-${estado.naoLidas}">Você tem <strong><c:out value="${estado.naoLidas}"/></strong>
                <c:out value="${estado.naoLidas == 1 ? 'advertência nova' : 'advertências novas'}"/> da cantina.
                <a href="conta.jsp#advertencias">Ler agora</a></cif:notificacao>
        </c:if>
    </div>
    <c:choose>
        <c:when test="${not estado.vinculado}">
            <div class="page-head"><h1>Catálogo</h1></div>
            <section class="cartao-vinculo" aria-labelledby="titulo-vinculo">
                <h2 id="titulo-vinculo">Vincule sua conta a uma cantina</h2>
                <p>Para ver e comprar produtos, informe o código que a cantina gera para você no momento.
                    Cada conta fica vinculada a uma cantina por vez.</p>
                <%@ include file="/WEB-INF/jspf/form-vinculo.jspf" %>
            </section>
        </c:when>
        <c:otherwise>
            <div class="page-head"><h1>Catálogo</h1></div>

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

            <div id="estado" class="estado carregando" role="status">Carregando produtos...</div>
            <div id="productContainer" class="grade" hidden></div>
        </c:otherwise>
    </c:choose>
</main>
<script src="js/util.js"></script>
<script src="js/vinculo.js"></script>
<script src="js/filtro.js"></script>
</body>
</html>
