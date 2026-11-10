// Vitrine do comprador: lista produtos por categoria e adiciona ao carrinho.
// Depende de util.js.

function carregarProdutosUser(categoria = 'todos') {
	fetch('apiUser/produtos?categoria=' + encodeURIComponent(categoria))
		.then(response => {
			if (!response.ok) {
				throw new Error('Resposta inesperada do servidor: ' + response.status);
			}
			return response.json();
		})
		.then(produtos => {
			const container = document.getElementById('productContainer');
			container.replaceChildren();
			if (produtos.length === 0) {
				container.appendChild(criar('p', { texto: 'Nenhum produto disponível nesta categoria.' }));
				return;
			}
			produtos.forEach(produto => container.appendChild(criarCartao(produto)));
		})
		.catch(error => {
			console.error('Erro ao buscar produtos:', error);
			const container = document.getElementById('productContainer');
			container.replaceChildren(criar('p', { texto: 'Não foi possível carregar os produtos. Tente novamente mais tarde.' }));
		});
}

function criarCartao(produto) {
	const cartao = criar('div', { classe: 'product' });
	cartao.dataset.type = produto.categoria ? produto.categoria.toLowerCase() : 'outros';

	const imagem = criar('img', { classe: 'product-image', attrs: { src: urlImagem(produto), alt: produto.nome || 'Produto' } });
	cartao.appendChild(criar('div', { classe: 'product-image-wrapper' }, [imagem]));

	cartao.appendChild(criar('div', { classe: 'product-info' }, [
		criar('h3', { texto: produto.nome }),
		criar('p', { texto: produto.descricao || '' }),
		criar('p', {}, [criar('strong', { texto: 'Preço: ' }), document.createTextNode(formatarPreco(produto.preco))]),
		criar('p', {}, [criar('strong', { texto: 'Disponível: ' }), document.createTextNode(String(produto.estoque))])
	]));

	const quantidade = criar('input', {
		attrs: { type: 'number', min: '1', max: String(produto.estoque), value: '1', 'aria-label': 'Quantidade' }
	});
	quantidade.id = 'quantidade-' + produto.id;
	quantidade.style.width = '60px';
	const botao = criar('button', { classe: 'add-to-cart', texto: 'Comprar', attrs: { type: 'button' } });
	botao.addEventListener('click', () => adicionarAoCarrinho(produto.id, quantidade));
	cartao.appendChild(criar('div', {}, [quantidade, botao]));
	return cartao;
}

// Adiciona o produto ao carrinho da sessao
function adicionarAoCarrinho(produtoId, campoQuantidade) {
	const quantidade = parseInt(campoQuantidade.value, 10);
	const mensagem = document.getElementById('mensagem');
	if (!Number.isInteger(quantidade) || quantidade < 1) {
		mostrarMensagem(mensagem, 'Informe uma quantidade válida.', true);
		return;
	}
	fetch('carrinho', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ produtoId: produtoId, quantidade: quantidade })
	})
		.then(response => response.json().catch(() => ({})).then(corpo => ({ ok: response.ok, corpo })))
		.then(({ ok, corpo }) => {
			mostrarMensagem(mensagem, corpo.message || (ok ? 'Produto adicionado ao carrinho!' : 'Erro ao adicionar produto.'), !ok);
		})
		.catch(() => mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true));
}

document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('.filter-btn').forEach(botao => {
		botao.addEventListener('click', function() {
			document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
			this.classList.add('active');
			carregarProdutosUser(this.getAttribute('data-filter'));
		});
	});
	carregarProdutosUser('todos');
});
