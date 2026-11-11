// Vitrine do comprador: busca, filtro por categoria e adicao ao carrinho. Depende de util.js.

const ROTULO_CATEGORIA = { salgado: 'Salgado', lanche: 'Lanche', doce: 'Doce', bebida: 'Bebida' };

const vitrine = { produtos: [], categoria: 'todos', busca: '' };

function carregarProdutosUser(categoria) {
	vitrine.categoria = categoria;
	const estado = document.getElementById('estado');
	const container = document.getElementById('productContainer');
	container.hidden = true;
	mostrarEstado(estado, 'Carregando produtos...', 'carregando');

	requisitar('apiUser/produtos?categoria=' + encodeURIComponent(categoria))
		.then(({ ok, corpo }) => {
			if (!ok || !Array.isArray(corpo)) throw new Error('resposta inesperada');
			vitrine.produtos = corpo;
			renderizar();
		})
		.catch(() => {
			mostrarEstado(estado, 'Não foi possível carregar os produtos.', 'erro',
				{ texto: 'Tentar novamente', fn: () => carregarProdutosUser(vitrine.categoria) });
		});
}

function renderizar() {
	const estado = document.getElementById('estado');
	const container = document.getElementById('productContainer');
	const termo = semAcento(vitrine.busca.trim());
	const lista = vitrine.produtos.filter(p => !termo || semAcento(p.nome + ' ' + (p.descricao || '')).includes(termo));

	container.replaceChildren();
	if (vitrine.produtos.length === 0) {
		container.hidden = true;
		mostrarEstado(estado, 'Nenhum produto cadastrado nesta categoria.', 'vazio');
		return;
	}
	if (lista.length === 0) {
		container.hidden = true;
		mostrarEstado(estado, 'Nenhum produto encontrado para "' + vitrine.busca.trim() + '".', 'vazio', {
			texto: 'Limpar busca',
			fn: () => { document.getElementById('busca').value = ''; vitrine.busca = ''; renderizar(); }
		});
		return;
	}
	mostrarEstado(estado, '', 'vazio');
	lista.forEach(p => container.appendChild(criarCartao(p)));
	container.hidden = false;
}

function criarCartao(produto) {
	const esgotado = produto.estoque < 1;
	const cartao = criar('article', { classe: 'produto' + (esgotado ? ' esgotado' : '') });
	cartao.dataset.type = produto.categoria || 'outros';

	const imagem = criar('img', { attrs: { src: urlImagem(produto), alt: produto.nome || 'Produto', loading: 'lazy' } });
	const areaImagem = criar('div', { classe: 'produto-img' }, [imagem]);
	if (esgotado) {
		areaImagem.appendChild(criar('span', { classe: 'selo selo-esgotado', texto: 'Esgotado' }));
	} else if (produto.estoque === 1) {
		areaImagem.appendChild(criar('span', { classe: 'selo selo-ultima', texto: 'Última unidade' }));
	}
	cartao.appendChild(areaImagem);

	const corpo = criar('div', { classe: 'produto-corpo' }, [
		criar('span', { classe: 'categoria-rotulo', texto: ROTULO_CATEGORIA[produto.categoria] || '' }),
		criar('h3', { classe: 'produto-nome', texto: produto.nome }),
		criar('p', { classe: 'produto-desc', texto: produto.descricao || '' }),
		criar('span', { classe: 'preco', texto: formatarPreco(produto.preco) })
	]);

	const acoes = criar('div', { classe: 'produto-acoes' });
	if (esgotado) {
		acoes.appendChild(criar('button', { classe: 'btn btn-bloco', texto: 'Esgotado', attrs: { type: 'button', disabled: '', 'aria-disabled': 'true' } }));
	} else {
		const campo = criar('input', {
			attrs: { type: 'number', min: '1', max: String(produto.estoque), value: '1', inputmode: 'numeric', 'aria-label': 'Quantidade de ' + produto.nome }
		});
		const ajustar = delta => {
			const novo = Math.min(produto.estoque, Math.max(1, (parseInt(campo.value, 10) || 1) + delta));
			campo.value = String(novo);
		};
		const menos = criar('button', { texto: '−', attrs: { type: 'button', 'aria-label': 'Diminuir quantidade' } });
		const mais = criar('button', { texto: '+', attrs: { type: 'button', 'aria-label': 'Aumentar quantidade' } });
		menos.addEventListener('click', () => ajustar(-1));
		mais.addEventListener('click', () => ajustar(1));
		const botao = criar('button', { classe: 'btn', texto: 'Adicionar', attrs: { type: 'button' } });
		botao.addEventListener('click', () => adicionarAoCarrinho(produto, campo));
		acoes.appendChild(criar('div', { classe: 'qtd' }, [menos, campo, mais]));
		acoes.appendChild(botao);
	}
	corpo.appendChild(acoes);
	cartao.appendChild(corpo);
	return cartao;
}

function adicionarAoCarrinho(produto, campo) {
	const quantidade = parseInt(campo.value, 10);
	const mensagem = document.getElementById('mensagem');
	if (!Number.isInteger(quantidade) || quantidade < 1 || quantidade > produto.estoque) {
		mostrarMensagem(mensagem, 'Informe uma quantidade entre 1 e ' + produto.estoque + '.', true);
		campo.focus();
		return;
	}
	requisitar('carrinho', {
		method: 'POST',
		headers: cabecalhosCsrf({ 'Content-Type': 'application/json' }),
		body: JSON.stringify({ produtoId: produto.id, quantidade: quantidade })
	})
		.then(({ ok, corpo }) => {
			mostrarMensagem(mensagem, ok ? produto.nome + ' adicionado ao carrinho.' : (corpo.message || 'Erro ao adicionar produto.'), !ok);
			if (ok) atualizarBadgeCarrinho();
			else if (corpo.message && corpo.message.indexOf('esgotado') >= 0) carregarProdutosUser(vitrine.categoria);
		})
		.catch(() => mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true));
}

document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('.chip[data-filter]').forEach(botao => {
		botao.addEventListener('click', function() {
			document.querySelectorAll('.chip[data-filter]').forEach(b => b.setAttribute('aria-pressed', String(b === botao)));
			carregarProdutosUser(botao.dataset.filter);
		});
	});
	document.getElementById('busca').addEventListener('input', debounce(e => {
		vitrine.busca = e.target.value;
		if (vitrine.produtos.length) renderizar();
	}, 150));
	carregarProdutosUser('todos');
});
