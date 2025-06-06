// Exibição de produtos
document.addEventListener("DOMContentLoaded", function () {
    fetch('api/produtos')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok ' + response.statusText);
            }
            return response.json();
        })
        .then(produtos => {
            const container = document.getElementById('productContainer');

            // Limpa o container antes de adicionar os produtos (útil se você chamar isso várias vezes)
            container.innerHTML = ''; 

            produtos.forEach(produto => {
                const produtoDiv = document.createElement('div');
                produtoDiv.className = 'product';
                // Adiciona um atributo data-type para filtragem
                produtoDiv.setAttribute('data-type', produto.categoria ? produto.categoria.toLowerCase() : 'outros'); 

                // Constrói a URL da imagem usando o ImageServlet e o ID do produto
                const imageUrl = produto.id ? `image?id=${produto.id}` : '#'; 

                // Decide se a imagem será exibida (se houver bytes de imagem)
                const imageTag = produto.imagem && produto.imagem.length > 0 
                                 ? `<img src="${imageUrl}" alt="${produto.nome}" class="product-image">`
                                 : `<img src="img/placeholder-image.png" alt="Imagem padrão" class="product-image">`; // Imagem padrão se não houver imagem

                produtoDiv.innerHTML = `
                    <div class="product-image-wrapper">
                        ${imageTag}
                    </div>
                    <div class="product-info">
                        <h3>${produto.nome}</h3>
                        <p>${produto.descricao}</p>
                        <p>R$ ${produto.preco.toFixed(2)}</p>
                    </div>
                    <form method="post" action="editar-produto" class="adicionar-ao-carrinho">
                        <input type="hidden" name="idProduto" value="${produto.id}">
                        <button type="submit" class="add-to-cart">Editar</button>
                    </form>
                `;

                container.appendChild(produtoDiv);
            });
        })
        .catch(error => {
            console.error('Erro ao buscar produtos:', error);
            const container = document.getElementById('productContainer');
            container.innerHTML = '<p>Não foi possível carregar os produtos. Tente novamente mais tarde.</p>';
        });
});
