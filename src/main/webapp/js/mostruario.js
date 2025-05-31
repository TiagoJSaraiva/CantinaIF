// Exibição de produtos
document.addEventListener("DOMContentLoaded", function () {
fetch('api/produtos')
    .then(response => response.json())
    .then(produtos => {
        const container = document.getElementById('productContainer');

        produtos.forEach(produto => {
            const produtoDiv = document.createElement('div');
            produtoDiv.className = 'product';

            produtoDiv.innerHTML = `
                <!-- Imagem se quiser ativar -->
                <!-- <img src="${produto.imageURL || '#'}" alt="${produto.nome}"> -->

                <div class="product-info">
                    <h3>${produto.nome}</h3>
                    <p>${produto.descricao}</p>
                    <p>R$ ${produto.preco.toFixed(2)}</p>
                </div>

                <form method="post" action="registrar-produto" class="adicionar-ao-carrinho">
                    <input type="hidden" name="idProduto" value="${produto.id}">
                    <button type="submit" class="add-to-cart">Editar</button>
                </form>
            `;

            container.appendChild(produtoDiv);
        });
    })
    .catch(error => {
        console.error('Erro ao buscar produtos:', error);
    });
});
