<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="estado" value="${requestScope.estadoComprador}"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Conta desativada - CantinaIF</title>
</head>
<body>
<header class="topbar">
    <div class="topbar-inner">
        <span class="brand"><img src="img/Logo.png" alt=""><span>CantinaIF</span></span>
        <div class="usuario">
            <span class="usuario-nome">Olá, <c:out value="${sessionScope.comprador.nome}"/></span>
            <form action="logout" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                <button type="submit" class="btn btn-ghost">Sair</button>
            </form>
        </div>
    </div>
</header>
<main class="container container-estreito" id="conteudo">
    <section class="alerta alerta-erro aviso-desativada" role="alert">
        <h1>Conta desativada</h1>
        <p>
            <c:choose>
                <c:when test="${not empty estado.vendedorNome}">A cantina <strong><c:out value="${estado.vendedorNome}"/></strong></c:when>
                <c:otherwise>A cantina</c:otherwise>
            </c:choose>
            desativou a sua conta. Enquanto ela estiver desativada você não pode ver produtos, comprar, acompanhar
            pedidos nem trocar de cantina. Procure a cantina para regularizar a situação.
        </p>
    </section>

    <section class="painel" aria-labelledby="titulo-adv">
        <h2 id="titulo-adv">Advertências recebidas</h2>
        <div id="estado-adv" class="estado carregando" role="status">Carregando...</div>
        <ul id="lista-adv" class="lista-advertencias" hidden></ul>
    </section>
</main>
<script src="js/util.js"></script>
<script src="js/advertencias.js"></script>
</body>
</html>
