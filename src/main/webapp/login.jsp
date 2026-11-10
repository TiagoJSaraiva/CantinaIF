<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %> <%-- <--- ADICIONE ESTA LINHA PARA USAR JSTL --%>

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <title>Login</title>
    <link rel="stylesheet" href="css/login.css">
    <style>
        .success-message { color: #155724; background-color: #d4edda; border: 1px solid #c3e6cb; padding: 10px; margin-bottom: 15px; border-radius: 5px; text-align: center; }
        /* Estilo da mensagem de erro. Incluir na pasta de CSS depois*/
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
        <h1>Login</h1>

        <!-- Área para exibir a mensagem de erro -->
        <c:if test="${param.cadastro == 'ok'}">
            <div class="success-message" role="status"><p>Cadastro realizado. Faça login para continuar.</p></div>
        </c:if>
        <c:if test="${not empty requestScope.error}">
            <div class="error-message">
                <p><c:out value="${requestScope.error}"/></p>
            </div>
        </c:if>

        <form class="login-form" action="login" method="post">
            <label for="prontuario">Prontuário</label>
            <input type="text" id="prontuario" name="prontuario" required maxlength="10">

            <label for="senha">Senha</label>
            <input type="password" id="senha" name="senha" required maxlength="100">

            <button type="submit">Login</button>
            <a href="cadastro.jsp"><button type="button">Cadastrar-se</button></a>
        </form>
    </section>
</body>
</html>