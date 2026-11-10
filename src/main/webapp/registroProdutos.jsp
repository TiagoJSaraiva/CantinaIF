<%@ page contentType="text/html;charset=UTF-8"%>

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Painel do Vendedor</title>
    <link rel="stylesheet" href="css/user.css">
</head>
<body>
    <div class="navbar">
        <div class="logo">
            <img src="img/Logo.png" alt="Logo" style="height: 40px;">
        </div>        
        <div>Painel do Vendedor</div>
        <div>
            <button class="new-product-btn" onclick="toggleNewProductForm()">+ Novo Produto</button>
            <a href="logout" style="color: white; margin-left: 20px;">Logout</a>
            <a href="pedidos.jsp" style="color: white; margin-left: 20px;">Pedidos</a>
        </div>
    </div>
    
    <div class="filters">
        <button class="filter-btn active" data-filter="todos">Todos</button>
        <button class="filter-btn" data-filter="salgado">Salgados</button>
        <button class="filter-btn" data-filter="doce">Doces</button>
        <button class="filter-btn" data-filter="lanche">Lanches</button>
        <button class="filter-btn" data-filter="bebida">Bebidas</button>
    </div>
    
    <div id="new-product-form" class="new-product-form">
        <form action="registrar-produto" method="post" enctype="multipart/form-data"> <h3>Novo Produto</h3>
        
            <input type="text" name="nome" placeholder="Nome do produto" required><br><br>
        
            <textarea name="descricao" placeholder="Descrição" required></textarea><br><br>
        
            <input type="number" name="preco" step="0.01" placeholder="Valor" required><br><br>
        
            <input type="number" name="estoque" placeholder="Quantidade inicial" required><br><br>
        
            <select name="categoria" required>
                <option value="" disabled selected>Selecione uma categoria</option>
                <option value="SALGADO">Salgado</option>
                <option value="DOCE">Doce</option>
                <option value="LANCHE">Lanche</option>
                <option value="BEBIDA">Bebida</option>
            </select><br><br>

            <label for="imagem">Imagem do Produto:</label>
            <input type="file" id="imagem" name="imagem" accept="image/*" required><br><br>
        
            <button type="submit">Criar</button>
        </form>
    </div>
    
    <div id="mensagem" role="status" style="display: none;"></div>
    <div id="productContainer" class="produtosPai">
        </div>
    <script>
    // Formulário de criação de novos produtos
    function toggleNewProductForm() {
        const form = document.getElementById('new-product-form');
        form.style.display = (form.style.display === 'block') ? 'none' : 'block';
    }
    </script>
    <script src="js/util.js"></script>
    <script src="js/ADMfiltro.js"></script>
</body>
</html>
