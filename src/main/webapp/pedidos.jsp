<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
    <meta name="csrf-token" content="<c:out value='${sessionScope.csrfToken}'/>">
<title>Painel de Pedidos - Vendedor</title>
<link rel="stylesheet" href="css/user.css">
</head>
<body>
    <div class="navbar">
        <div class="logo">
            <img src="img/Logo.png" alt="Logo" style="height: 40px;">
        </div>        
        <div>Painel do Vendedor</div>
        <div>
            <form action="logout" method="post" class="logout-form"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>"><button type="submit" class="link-button">Logout</button></form>
            <a href="registroProdutos.jsp" style="color: white;">Menu</a>
        </div>
    </div>

    <div id="pedidosContainer">
        <h2>Pedidos Pendentes</h2>
        <div id="loadingMessage">Carregando pedidos...</div>
        <div id="errorMessage" style="display: none;"></div>
        <div id="noPedidosMessage" style="display: none;">Nenhum pedido pendente no momento.</div>
        <!-- Os pedidos serão inseridos aqui pelo JavaScript -->
    </div>

    <script src="js/util.js"></script>
    <script src="js/pedidos.js">
    </script>
</body>
</html>