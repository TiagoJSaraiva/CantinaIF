<%@ page contentType="text/html;charset=UTF-8" %>
<!-- %@ taglib uri="http://jakarta.ee/jsp/jstl/core" prefix="c" %> --> <!-- JSTL com Jakarta EE -->
<%@ page import="model.Comprador" %> <!-- Importa classe Comprador para uso na sessão -->

<!DOCTYPE html>
<html lang="pt-br">
	<head>
	    <meta charset="UTF-8">
	    <title>Home - Cantina Universitária</title>
	    <link rel="stylesheet" href="css/home.css"> <!-- Link do CSS -->
	</head>
<body>
	<!-- Barra superior com logo, prontuário do usuário e links -->
	<div class="navbar">
	    <div class="logo">IF</div>
	    <div class="info">
	        <!-- Mostra o prontuário do comprador salvo na sessão -->
	        <span>Prontuário: ${sessionScope.comprador.prontuario}</span>
	        <a href="carrinho.jsp">Carrinho</a>
	        <a href="login.jsp">Logout</a>
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
	<div id="productContainer">
	    <!--  <c:forEach var="produto" items="${produtos}">-->
	        <!-- O data-type será usado no JavaScript para filtrar os produtos por categoria -->
	        <div class="product" data-type="${produto.categoria.name().toLowerCase()}">
	            <!-- Imagem do produto -->
	            <img src="${produto.imagemURL}" alt="${produto.nome}">
	            
	            <!-- Informações do produto -->
	            <div class="product-info">
	                <h3>${produto.nome}</h3>
	                <p>${produto.descricao}</p>
	                <p>R$ ${produto.preco}</p>
	            </div>
	
	            <!-- Formulário para adicionar produto ao carrinho -->
	            <form method="post" action="CarrinhoServlet">
	                <input type="hidden" name="idProduto" value="${produto.id}">
	                <button type="submit" class="add-to-cart">Adicionar</button>
	            </form>
	        </div>
	    <!-- </c:forEach> -->
	</div>
	
	<!-- Script JS para controlar o filtro por categoria -->
	<script>
	    const filterButtons = document.querySelectorAll('.filter-btn');
	    const products = document.querySelectorAll('.product');
	
	    filterButtons.forEach(button => {
	        button.addEventListener('click', () => {
	            // Marca o botão ativo
	            filterButtons.forEach(btn => btn.classList.remove('active'));
	            button.classList.add('active');
	
	            const filter = button.getAttribute('data-filter');
	
	            // Mostra ou oculta produtos com base no filtro
	            products.forEach(product => {
	                const type = product.getAttribute('data-type');
	                if (filter === 'todos' || filter === type) {
	                    product.style.display = 'inline-block';
	                } else {
	                    product.style.display = 'none';
	                }
	            });
	        });
	    });
	</script>
</body>
</html>
