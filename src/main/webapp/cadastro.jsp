<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cadastro</title>
    <link rel="stylesheet" href="css/login.css">
    <style>
        .error-message {
            color: #dc3545;
            background-color: #f8d7da;
            border: 1px solid #f5c6cb;
            padding: 10px;
            margin-bottom: 15px;
            border-radius: 5px;
            text-align: center;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <section class="login-page">
        <h1>Cadastro</h1>

        <c:if test="${not empty requestScope.error}">
            <div class="error-message" role="alert">
                <p><c:out value="${requestScope.error}"/></p>
            </div>
        </c:if>

        <form class="login-form" action="register" method="post">
            <label for="prontuario">Prontuário</label>
            <input type="text" id="prontuario" name="prontuario" required maxlength="10" value="<c:out value='${requestScope.prontuario}'/>">

            <label for="nome">Seu nome</label>
            <input type="text" id="nome" name="nome" required maxlength="75" value="<c:out value='${requestScope.nome}'/>">

            <label for="email">E-mail institucional</label>
            <input type="email" id="email" name="email" required maxlength="100" value="<c:out value='${requestScope.email}'/>">

            <label for="senha">Senha (mínimo 8 caracteres)</label>
            <input type="password" id="senha" name="senha" required minlength="8" maxlength="72">

            <button type="submit">Cadastro</button>
            <a href="login.jsp"><button type="button">Voltar pro login</button></a>
        </form>
    </section>
</body>
</html>
