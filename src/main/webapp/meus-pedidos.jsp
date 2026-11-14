<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="meus-pedidos"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Meus pedidos - CantinaIF</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo" data-novo="<c:out value='${param.novo}'/>">
    <div class="page-head"><h1>Meus pedidos</h1></div>


    <div id="estado" class="estado carregando" role="status">Carregando pedidos...</div>
    <div id="lista" class="lista-pedidos" hidden></div>
</main>
<script src="js/util.js"></script>
<script src="js/meusPedidos.js"></script>
</body>
</html>
