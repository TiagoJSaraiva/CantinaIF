// Painel do vendedor: lista todos os produtos, edita dados e ajusta estoque.
// Depende de util.js.

function carregarProdutosADM(categoria = 'todos') {
	fetch('apiAdm/produtos?categoria=' + encodeURIComponent(categoria))
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
				container.appendChild(criar('p', { texto: 'Nenhum produto cadastrado nesta categoria.' }));
				return;
			}
			produtos.forEach(produto => container.appendChild(criarCartaoADM(produto)));
		})
		.catch(error => {
			console.error('Erro ao buscar produtos:', error);
			const container = document.getElementById('productContainer');
			container.replaceChildren(criar('p', { texto: 'Não foi possível carregar os produtos. Tente novamente mais tarde.' }));
		});
}

function postForm(url, dados) {
	return fetch(url, {
		method: 'POST',
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
		body: new URLSearchParams(dados).toString()
	}).then(response => response.json().catch(() => ({})).then(corpo => ({ ok: response.ok, corpo })));
}

function criarCartaoADM(produto) {
	const mensagem = document.getElementById('mensagem');
	const cartao = criar('div', { classe: 'product' });
	cartao.dataset.type = produto.categoria ? produto.categoria.toLowerCase() : 'outros';

	const imagem = criar('img', { classe: 'product-image', attrs: { src: urlImagem(produto), alt: produto.nome || 'Produto' } });
	cartao.appendChild(criar('div', { classe: 'product-image-wrapper' }, [imagem]));

	cartao.appendChild(criar('div', { classe: 'product-info' }, [
		criar('h3', { texto: produto.nome }),
		criar('p', { texto: produto.descricao || '' }),
		criar('p', {}, [criar('strong', { texto: 'Preço: ' }), document.createTextNode(formatarPreco(produto.preco))])
	]));

	// Ajuste rapido de estoque
	const campoEstoque = criar('input', { attrs: { type: 'number', min: '0', value: String(produto.estoque), 'aria-label': 'Estoque' } });
	campoEstoque.id = 'qty-' + produto.id;
	const salvarEstoque = valor => {
		if (!Number.isInteger(valor) || valor < 0) return;
		postForm('atualizarEstoque', { id: produto.id, estoque: valor }).then(({ ok, corpo }) => {
			if (ok) campoEstoque.value = String(valor);
			mostrarMensagem(mensagem, corpo.message || (ok ? 'Estoque atualizado.' : 'Erro ao atualizar o estoque.'), !ok);
		}).catch(() => mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true));
	};
	const menos = criar('button', { classe: 'save-qty-btn', texto: '-', attrs: { type: 'button', 'aria-label': 'Diminuir estoque' } });
	const mais = criar('button', { classe: 'save-qty-btn', texto: '+', attrs: { type: 'button', 'aria-label': 'Aumentar estoque' } });
	menos.addEventListener('click', () => salvarEstoque(parseInt(campoEstoque.value, 10) - 1));
	mais.addEventListener('click', () => salvarEstoque(parseInt(campoEstoque.value, 10) + 1));
	campoEstoque.addEventListener('change', () => salvarEstoque(parseInt(campoEstoque.value, 10)));
	cartao.appendChild(criar('div', { classe: 'qty-controls' }, [
		criar('span', { texto: 'Estoque:' }), menos, campoEstoque, mais
	]));

	// Edicao completa
	const formulario = criarFormularioEdicao(produto, mensagem);
	const editar = criar('button', { classe: 'add-to-cart', texto: 'Editar', attrs: { type: 'button' } });
	editar.addEventListener('click', () => {
		formulario.style.display = (formulario.style.display === 'block') ? 'none' : 'block';
	});
	cartao.appendChild(editar);
	cartao.appendChild(formulario);
	return cartao;
}

function criarFormularioEdicao(produto, mensagem) {
	const form = criar('form', { classe: 'edit-form' });
	const nome = criar('input', { attrs: { type: 'text', name: 'nome', required: '', maxlength: '100', 'aria-label': 'Nome' } });
	nome.value = produto.nome || '';
	const descricao = criar('textarea', { attrs: { name: 'descricao', maxlength: '400', 'aria-label': 'Descrição' } });
	descricao.value = produto.descricao || '';
	const preco = criar('input', { attrs: { type: 'number', name: 'preco', step: '0.01', min: '0', required: '', 'aria-label': 'Preço' } });
	preco.value = String(produto.preco);
	const estoque = criar('input', { attrs: { type: 'number', name: 'estoque', min: '0', required: '', 'aria-label': 'Estoque' } });
	estoque.value = String(produto.estoque);
	const categoria = criar('select', { attrs: { name: 'categoria', 'aria-label': 'Categoria' } });
	['salgado', 'doce', 'lanche', 'bebida'].forEach(c => {
		const opcao = criar('option', { texto: c.charAt(0).toUpperCase() + c.slice(1), attrs: { value: c } });
		if (c === produto.categoria) opcao.selected = true;
		categoria.appendChild(opcao);
	});
	const salvar = criar('button', { texto: 'Salvar', attrs: { type: 'submit' } });
	[nome, descricao, preco, estoque, categoria, salvar].forEach(campo => {
		form.appendChild(campo);
		form.appendChild(document.createElement('br'));
	});
	form.addEventListener('submit', evento => {
		evento.preventDefault();
		postForm('editar-produto', {
			id: produto.id,
			nome: nome.value,
			descricao: descricao.value,
			preco: preco.value,
			estoque: estoque.value,
			categoria: categoria.value
		}).then(({ ok, corpo }) => {
			mostrarMensagem(mensagem, corpo.message || (ok ? 'Produto atualizado.' : 'Erro ao atualizar o produto.'), !ok);
			if (ok) {
				const ativo = document.querySelector('.filter-btn.active');
				carregarProdutosADM(ativo ? ativo.getAttribute('data-filter') : 'todos');
			}
		}).catch(() => mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true));
	});
	return form;
}

document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('.filter-btn').forEach(botao => {
		botao.addEventListener('click', function() {
			document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
			this.classList.add('active');
			carregarProdutosADM(this.getAttribute('data-filter'));
		});
	});
	carregarProdutosADM('todos');
});
