<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="csrf-token" content="<c:out value='${sessionScope.csrfToken}'/>">
    <title>Home - Cantina Universitária</title>
    <link rel="stylesheet" href="css/user.css"> <!-- Link do CSS -->
</head>
<body>

<!-- Barra superior com logo, prontuário do usuário e links -->
<div class="navbar">
    <div class="logo">
        	<a href="home.jsp"><img src="img/Logo.png" alt="Logo" style="height: 40px;"></a>
    </div>    
    <div class="info">
        <!-- Mostra o prontuário do comprador salvo na sessão -->
        <span>Prontuário: <c:out value="${sessionScope.comprador.prontuario}"/></span>
        <a href="Carrinho.jsp" style="
        font-weight: bold;
		text-decoration: none;
  		color: white;">Carrinho</a>
        <form action="logout" method="post" class="logout-form"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>"><button type="submit" class="link-button">Logout</button></form>
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
<div id="mensagem" role="status" style="display: none;"></div>
<div id="productContainer" class="produtosPai">
        
</div>

<!-- Script JS para controlar o filtro por categoria -->
<script src="js/util.js"></script>
<script src="js/filtro.js"></script>

</body>
</html>
