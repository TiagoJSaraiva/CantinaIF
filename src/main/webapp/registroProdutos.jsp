<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> <!-- JSTL com Jakarta EE -->

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Painel do Vendedor</title>
    <link rel="stylesheet" href="css/adm.css"> <!-- Link para o arquivo de estilização adm.css -->
    <script>
        // Formulário de edição
        function toggleEditForm(id) {
            document.getElementById('edit-form-' + id).style.display = 'block';
        }

        // Formulário de criação de novos produtos
        function toggleNewProductForm() {
            const form = document.getElementById('new-product-form');
            form.style.display = (form.style.display === 'none') ? 'block' : 'none';
        }

        // Função para atualização manual de quantidade de itens disponíveis
        function updateQuantity(id, action) {
            const input = document.getElementById('qty-' + id);
            let value = parseInt(input.value);
            if (action === 'up') value++;
            if (action === 'down' && value > 0) value--;

            input.value = value;

            fetch('atualizarQuantidade?id=' + id + '&quantidade=' + value, { method: 'POST' });
        }
    </script>
</head>
<body>
    <!-- Barra de navegação -->
    <div class="navbar">
        <div class="logo">IF</div>       
        <div>Painel do Vendedor</div>
        <div>
            <button class="new-product-btn" onclick="toggleNewProductForm()">+ Novo Produto</button>
            <a href="logout" style="color: white; margin-left: 20px;">Logout</a>
        </div>
    </div>
    
    <!-- Menu de filtragem por tipo de produto -->
    <div class="filters">
        <button class="filter-btn active" data-filter="todos">Todos</button>
        <button class="filter-btn" data-filter="salgado">Salgados</button>
        <button class="filter-btn" data-filter="doce">Doces</button>
        <button class="filter-btn" data-filter="lanche">Lanches</button>
        <button class="filter-btn" data-filter="bebida">Bebidas</button>
    </div>
    
    <!-- Novo produto -->
    <div id="new-product-form" class="new-product-form">
        <form action="registrar-produto" method="post">
            <h3>Novo Produto</h3>
            <input type="text" name="nome" placeholder="Nome do produto" required><br><br>
            <textarea name="descricao" placeholder="Descrição" required></textarea><br><br>
            <input type="number" name="valor" step="0.01" placeholder="Valor" required><br><br>
            <input type="number" name="quantidade" placeholder="Quantidade inicial" required><br><br>
            <button type="submit">Criar</button>
        </form>
    </div>
    
    <!-- Lista de produtos renderizados dinamicamente -->
    <div id="productContainer">
        <c:forEach var="produto" items="${produtos}">
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
            <!-- Formulário de edição -->
            <div id="edit-form-${produto.id}" class="edit-form">
                <form action="editarProduto" method="post">
                    <input type="hidden" name="id" value="${produto.id}">
                    <input type="text" name="nome" value="${produto.nome}" required><br><br>
                    <textarea name="descricao" required>${produto.descricao}</textarea><br><br>
                    <input type="number" name="valor" step="0.01" value="${produto.valor}" required><br><br>
                    <button type="submit">Salvar Alterações</button>
                </form>
            </div>
        </c:forEach>
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
