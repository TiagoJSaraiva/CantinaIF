<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="model.Comprador"%> <!--Para pegar os dados do usuário-->
<%@ page import="model.Produto%>    <!--Para pegar os dados dos produtos-->
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Home - Cantina Universitária</title>
    <link rel="stylesheet" href="styles.css"> <!--Link para o arquivo de estilização Styles.css-->
</head>
<body>

<!--Barra superior do aplicativo-->
<div class="navbar">
    <div class="logo">IF</div>
    <div class="info">
        <span>Prontuário: ${sessionScope.Compador.prontuario}</span> <!--pega o prontuário do Comprador ativo na seção-->
        <a href="carrinho.jsp">Carrinho</a>
        <a href="logout">Logout</a>
    </div>
</div>

<!--Menu de filtragem por tipo de produto-->
<div class="filters">
    <button class="filter-btn active" data-filter="todos">Todos</button>
    <button class="filter-btn" data-filter="salgado">Salgados</button>
    <button class="filter-btn" data-filter="doce">Doces</button>
    <button class="filter-btn" data-filter="lanche">Lanches</button>
    <button class="filter-btn" data-filter="bebida">Bebidas</button>
</div>

<!--O código abaixo percorre a lista de produtos disponíveis e cria um div para cada no padrão presente entre as linhas 31 e 44-->
<div class="container" id="productContainer">
    <c:forEach var="produto" items="${produtos}"><!--Recebe a lista de produtos enviada pelo servlet e exibe na estrutura abaixo-->
        <div class="product" data-type="${produto.tipo}">
            <img src="${produto.imagemURL}" alt="${Produto.nome}">
            <h3>${Produto.nome}</h3>
            <p>${Produto.descricao}</p>
            <p>R$ ${Produto.preco}</p>
            <form method="post" action="CarrinhoServlet"><!--Esse formulário é feito para enviar ao carrinho o item desejado ao clicar no botão para adicionar-->
                <input type="hidden" name="idProduto" value="${Produto.id}"><!--***DEVEMOS COLOCAR UM ID GERADO AUTOMATICAMENTE E UNIQUE PARA CADA PRODUTO E PARA MELHOR INTEGRIDADE DO SISTEMA***-->
                <button type="submit">Adicionar</button>
            </form>
        </div>
    </c:forEach>
</div>

<!-- Script para fazer a filtragem -->
<script>
    const filterButtons = document.querySelectorAll('.filter-btn');
    const products = document.querySelectorAll('.product');

    filterButtons.forEach(button => {
        button.addEventListener('click', () => {
            filterButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');

            const filter = button.getAttribute('data-filter');

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

<!--Observações:

    A variável ${produtos} deve ser uma List<Produto> passada como atributo da request no servlet/controller.
        (tipo fazer um método/função no ProdutoDAO para listar todos os produtos (com quantidade > 0) 
        e depois o ProdutoServlet manda mandar essa lista pro jsp com algo tipo:
            "request.setAttribute("produtos", listaProdutos);
             request.getRequestDispatcher("/home.jsp").forward(request, response);")

    A URL "adicionarCarrinho" pode apontar para um servlet ou endpoint que lide com o carrinho.
    -->

</body>
</html>
