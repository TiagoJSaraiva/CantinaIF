<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Carrinho de Compras</title>
    <style>
        /* ... Seus estilos existentes ... */
        .mensagem-sucesso {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
            padding: 10px;
            margin-bottom: 20px;
            text-align: center;
            border-radius: 5px;
        }
        .mensagem-erro {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
            padding: 10px;
            margin-bottom: 20px;
            text-align: center;
            border-radius: 5px;
        }
    </style>
    <meta charset="UTF-8">
    <title>Carrinho - Cantina Universitária</title>
    <link rel="stylesheet" href="css/user.css">
</head>
<body>

<div class="navbar">
    <div class="logo">
        <a href="home.jsp"><img src="img/Logo.png" alt="Logo" style="height: 40px;"></a>
    </div>    
    <div class="info">
        <span>Prontuário: ${sessionScope.comprador.prontuario}</span>
        <a href="Carrinho.jsp" style="font-weight: bold; text-decoration: none; color: white;">Carrinho</a>
        <a href="logout" style="font-weight: bold; text-decoration: none; color: white;">Logout</a>
    </div>
</div>

    <h1>Seu Carrinho</h1>

    <!-- Área para exibir mensagens -->
    <div id="message-area">
        <%-- Verifica se há uma mensagem de sucesso na sessão --%>
        <c:if test="${not empty sessionScope.successMessage}">
            <div class="mensagem-sucesso">
                <p>${sessionScope.successMessage}</p>
            </div>
            <%-- Remove a mensagem da sessão para que não apareça novamente na próxima visita --%>
            <c:remove var="successMessage" scope="session"/>
        </c:if>
        
        <%-- Verifica se há uma mensagem de erro na requisição (do forward do Servlet) --%>
        <c:if test="${not empty requestScope.errorMessage}">
            <div class="mensagem-erro">
                <p>${requestScope.errorMessage}</p>
            </div>
            <%-- Não é necessário remover, pois requestScope é limpo a cada nova requisição --%>
        </c:if>
    </div>

  <div id="carrinho-container"></div>
  <!-- REMOVA ESTA LINHA: <div class="resumo" id="resumo-total"></div> -->

        <div class="finalizar">
            <p><strong>Total: <span id="display-total">R$ 0,00</span></strong></p> <%-- Este é o único total que vamos usar --%>
            <form action="FinalizarCompraServlet" method="post">
                <button type="submit">FINALIZAR COMPRA</button>
            </form>
        </div>
        
        <script src="js/util.js"></script>
        <script src="js/Carrinho.js"></script>
</body>
</html>