<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="model.Comprador" %> <!-- Importa classe Comprador para uso na sessão -->
<%@ page import="model.Produto" %>

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Home - Cantina Universitária</title>
    <link rel="stylesheet" href="css/user.css"> <!-- Link do CSS -->
</head>
<body>

<!-- Barra superior com logo, prontuário do usuário e links -->
<div class="navbar">
    <div class="logo">
        	<img src="img/Logo.png" alt="Logo" style="height: 40px;">
    </div>    
    <div class="info">
        <!-- Mostra o prontuário do comprador salvo na sessão -->
        <span>Prontuário: ${sessionScope.comprador.prontuario}</span>
        <a href="carrinho.jsp" style="
        font-weight: bold;
		text-decoration: none;
  		color: white;">Carrinho</a>
        <a href="login.jsp" style="
        font-weight: bold;
		text-decoration: none;
  		color: white;">Logout</a>
    </div>
</div>

<!-- Botões de filtro por categoria -->
<div class="filters">
    <button class="filter-btn active" data-filter="todos">Todos</button>
    <button class="filter-btn" data-filter="salgado">Salgados</button>
    <button class="filter-btn" data-filter="doce">Doces</button>
    <button class="filter-btn" data-filter="lanche">Lanches</button>
    <button class="filter-btn" data-filter="bebida">Bebidas</button>
</div>

<!-- Lista de produtos renderizados dinamicamente -->
<div id="productContainer" class="produtosPai">
        
</div>

<!-- Script JS para controlar o filtro por categoria -->
<script src="js/mostruario.js"></script>

</body>
</html>
