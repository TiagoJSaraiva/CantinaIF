<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Cadastro - CantinaIF</title>
</head>
<body>
<main class="auth" id="conteudo">
    <section class="auth-card" aria-labelledby="titulo">
        <div class="brand-grande"><img src="img/Logo.png" alt=""><span>CantinaIF</span></div>
        <h1 id="titulo">Criar conta</h1>

        <c:if test="${not empty requestScope.error}">
            <div class="alerta alerta-erro" role="alert"><p><c:out value="${requestScope.error}"/></p></div>
        </c:if>

        <form action="register" method="post" novalidate data-validar>
            <input type="hidden" name="_csrf" value="<c:out value='${requestScope.csrfForm}'/>">

            <div class="field">
                <label for="prontuario">Prontuário</label>
                <input type="text" id="prontuario" name="prontuario" required maxlength="10" pattern="[A-Za-z0-9]{1,10}"
                       data-msg-pattern="Use apenas letras e números (até 10 caracteres)." autocomplete="username"
                       value="<c:out value='${requestScope.prontuario}'/>"
                       <c:if test="${not empty requestScope.erros.prontuario}">aria-invalid="true" aria-describedby="erro-prontuario"</c:if>>
                <span class="campo-erro" id="erro-prontuario"><c:out value="${requestScope.erros.prontuario}"/></span>
            </div>

            <div class="field">
                <label for="nome">Seu nome</label>
                <input type="text" id="nome" name="nome" required maxlength="75" autocomplete="name"
                       value="<c:out value='${requestScope.nome}'/>"
                       <c:if test="${not empty requestScope.erros.nome}">aria-invalid="true" aria-describedby="erro-nome"</c:if>>
                <span class="campo-erro" id="erro-nome"><c:out value="${requestScope.erros.nome}"/></span>
            </div>

            <div class="field">
                <label for="email">E-mail institucional</label>
                <input type="email" id="email" name="email" required maxlength="100" autocomplete="email"
                       value="<c:out value='${requestScope.email}'/>"
                       <c:if test="${not empty requestScope.erros.email}">aria-invalid="true" aria-describedby="erro-email"</c:if>>
                <span class="campo-erro" id="erro-email"><c:out value="${requestScope.erros.email}"/></span>
            </div>

            <div class="field">
                <label for="senha">Senha</label>
                <input type="password" id="senha" name="senha" required minlength="8" maxlength="72" autocomplete="new-password"
                       <c:if test="${not empty requestScope.erros.senha}">aria-invalid="true" aria-describedby="erro-senha"</c:if>>
                <span class="dica">Mínimo de 8 caracteres.</span>
                <span class="campo-erro" id="erro-senha"><c:out value="${requestScope.erros.senha}"/></span>
            </div>

            <button type="submit" class="btn btn-bloco">Cadastrar</button>
        </form>
        <div class="auth-links">
            <a class="btn btn-secundario btn-bloco" href="login.jsp">Voltar para o login</a>
        </div>
    </section>
</main>
<script src="js/util.js"></script>
<script src="js/login.js"></script>
</body>
</html>
