<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" session="false"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%-- Pagina de erro generica: nunca exibe detalhes da excecao nem stack trace. --%>
<c:set var="codigo" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="icon" type="image/png" href="img/Logo.png">
    <link rel="stylesheet" href="css/app.css">
    <title>Ops - CantinaIF</title>
</head>
<body>
<main class="auth" id="conteudo">
    <section class="auth-card" aria-labelledby="titulo">
        <div class="brand-grande"><img src="img/Logo.png" alt=""><span>CantinaIF</span></div>
        <h1 id="titulo">
            <c:choose>
                <c:when test="${codigo == 404}">Página não encontrada</c:when>
                <c:when test="${codigo == 403}">Acesso negado</c:when>
                <c:when test="${codigo == 429}">Muitas requisições</c:when>
                <c:when test="${codigo == 400 || codigo == 405}">Requisição inválida</c:when>
                <c:otherwise>Algo deu errado</c:otherwise>
            </c:choose>
        </h1>
        <p>
            <c:choose>
                <c:when test="${codigo == 404}">O endereço que você tentou acessar não existe.</c:when>
                <c:when test="${codigo == 429}">Você fez requisições demais em pouco tempo. Aguarde um minuto e tente de novo.</c:when>
                <c:when test="${codigo == 403}">Você não tem permissão para esta ação, ou sua sessão expirou. Volte e tente novamente.</c:when>
                <c:when test="${codigo == 400 || codigo == 405}">Não foi possível processar o pedido.</c:when>
                <c:otherwise>Ocorreu um erro inesperado. Tente novamente em instantes.</c:otherwise>
            </c:choose>
        </p>
        <div class="auth-links">
            <a class="btn btn-bloco" href="login.jsp">Voltar ao início</a>
        </div>
    </section>
</main>
</body>
</html>
