<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="conta"/>
<c:set var="estado" value="${requestScope.estadoComprador}"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Minha conta - CantinaIF</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container container-estreito" id="conteudo">
    <div class="page-head"><h1>Minha conta</h1></div>
    <div id="mensagem" role="status" aria-live="polite" hidden></div>

    <section class="painel" aria-labelledby="titulo-dados">
        <h2 id="titulo-dados">Seus dados</h2>
        <dl class="dados">
            <dt>Nome</dt><dd><c:out value="${sessionScope.comprador.nome}"/></dd>
            <dt>Prontuário</dt><dd><c:out value="${sessionScope.comprador.prontuario}"/></dd>
            <dt>E-mail</dt><dd><c:out value="${sessionScope.comprador.email}"/></dd>
        </dl>
    </section>

    <section class="painel" aria-labelledby="titulo-cantina">
        <h2 id="titulo-cantina">Cantina vinculada</h2>
        <c:choose>
            <c:when test="${estado.vinculado}">
                <p class="cantina-atual"><strong><c:out value="${estado.vendedorNome}"/></strong></p>
                <p class="dica">Para comprar de outra cantina, informe um código gerado por ela. Ao trocar, o carrinho é
                    esvaziado; seus pedidos antigos continuam em Meus pedidos.</p>
            </c:when>
            <c:otherwise>
                <p>Nenhuma. Você só verá produtos depois de se vincular a uma cantina.</p>
            </c:otherwise>
        </c:choose>
        <%@ include file="/WEB-INF/jspf/form-vinculo.jspf" %>
    </section>

    <section class="painel" id="advertencias" aria-labelledby="titulo-adv">
        <div class="painel-topo">
            <h2 id="titulo-adv">Advertências</h2>
            <button type="button" class="btn btn-secundario btn-pequeno" id="btn-lidas" hidden>Marcar como lidas</button>
        </div>
        <div id="estado-adv" class="estado carregando" role="status">Carregando...</div>
        <ul id="lista-adv" class="lista-advertencias" hidden></ul>
    </section>
</main>
<script src="js/util.js"></script>
<script src="js/vinculo.js"></script>
<script src="js/advertencias.js"></script>
</body>
</html>
