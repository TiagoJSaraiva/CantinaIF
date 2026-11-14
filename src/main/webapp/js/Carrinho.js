// Tela do carrinho: lista, altera quantidade, remove itens e mostra o total. Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	const estado = document.getElementById('estado');
	const container = document.getElementById('carrinho-container');
	const resumo = document.getElementById('resumo');
	const total = document.getElementById('display-total');
	const badge = document.getElementById('badge-carrinho');

	function carregar() {
		mostrarEstado(estado, 'Carregando carrinho...', 'carregando');
		container.hidden = true;
		resumo.hidden = true;
		requisitar('carrinho')
			.then(({ ok, corpo }) => {
				if (!ok || !Array.isArray(corpo)) throw new Error('resposta inesperada');
				renderizar(corpo);
			})
			.catch(() => mostrarEstado(estado, 'Erro ao carregar o carrinho.', 'erro', { texto: 'Tentar novamente', fn: carregar }));
	}

	function renderizar(itens) {
		container.replaceChildren();
		if (badge) badge.textContent = String(itens.reduce((s, i) => s + i.quantidade, 0));
		if (itens.length === 0) {
			container.hidden = true;
			resumo.hidden = true;
			estado.replaceChildren();
			estado.className = 'estado';
			estado.hidden = false;
			estado.appendChild(document.createTextNode('Seu carrinho está vazio.'));
			estado.appendChild(document.createElement('br'));
			estado.appendChild(criar('a', { classe: 'btn', texto: 'Ver produtos', attrs: { href: 'home.jsp' } }));
			return;
		}
		mostrarEstado(estado, '', 'vazio');
		let totalGeral = 0;
		itens.forEach(item => {
			totalGeral += item.preco * item.quantidade;
			container.appendChild(criarItem(item));
		});
		total.textContent = formatarPreco(totalGeral);
		container.hidden = false;
		resumo.hidden = false;
	}

	function criarItem(item) {
		const imagem = criar('img', { attrs: { src: urlImagem(item), alt: item.nome || 'Produto', loading: 'lazy' } });
		const campo = criar('input', {
			attrs: { type: 'number', min: '1', max: String(item.estoque), value: String(item.quantidade), inputmode: 'numeric', 'aria-label': 'Quantidade de ' + item.nome }
		});
		const definir = quantidade => {
			if (!Number.isInteger(quantidade) || quantidade < 1) { campo.value = String(item.quantidade); return; }
			requisitar('carrinho', {
				method: 'PUT',
				headers: cabecalhosCsrf({ 'Content-Type': 'application/json' }),
				body: JSON.stringify({ produtoId: item.id, quantidade: quantidade })
			}).then(({ ok, corpo }) => {
				if (ok) carregar();
				else { notificar(corpo.message || 'Não foi possível alterar a quantidade.', 'erro'); campo.value = String(item.quantidade); }
			}).catch(() => notificar('Erro de comunicação com o servidor.', 'erro'));
		};
		const menos = criar('button', { texto: '−', attrs: { type: 'button', 'aria-label': 'Diminuir quantidade de ' + item.nome } });
		const mais = criar('button', { texto: '+', attrs: { type: 'button', 'aria-label': 'Aumentar quantidade de ' + item.nome } });
		if (item.quantidade <= 1) menos.disabled = true;
		menos.addEventListener('click', () => definir(item.quantidade - 1));
		mais.addEventListener('click', () => definir(item.quantidade + 1));
		campo.addEventListener('change', () => definir(parseInt(campo.value, 10)));

		const remover = criar('button', { classe: 'btn btn-perigo btn-pequeno', texto: 'Remover', attrs: { type: 'button', 'aria-label': 'Remover ' + item.nome + ' do carrinho' } });
		remover.addEventListener('click', () => {
			requisitar('carrinho?produtoId=' + encodeURIComponent(item.id), { method: 'DELETE', headers: cabecalhosCsrf() })
				.then(({ ok, corpo }) => {
					if (ok) { notificar(item.nome + ' removido do carrinho.'); carregar(); }
					else notificar(corpo.message || 'Não foi possível remover o item.', 'erro');
				})
				.catch(() => notificar('Erro de comunicação com o servidor.', 'erro'));
		});

		const cartao = criar('article', { classe: 'item-carrinho' }, [
			imagem,
			criar('div', {}, [
				criar('h3', { texto: item.nome }),
				criar('p', { classe: 'dica', texto: formatarPreco(item.preco) + ' cada' })
			]),
			criar('div', { classe: 'item-linha' }, [
				criar('div', { classe: 'qtd' }, [menos, campo, mais]),
				remover,
				criar('span', { classe: 'subtotal', texto: formatarPreco(item.preco * item.quantidade) })
			])
		]);
		return cartao;
	}

	document.getElementById('form-finalizar').addEventListener('submit', () => {
		const botao = document.getElementById('btn-finalizar');
		botao.disabled = true;
		botao.textContent = 'Enviando pedido...';
	});

	carregar();
});
