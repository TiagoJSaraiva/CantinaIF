<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
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
            <a href="logout" style="color: white;">Logout</a>
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

    <script src="js/pedidos.js">
    </script>
</body>
</html>