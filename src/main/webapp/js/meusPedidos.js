// "Meus pedidos": status e historico do comprador. Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	const estado = document.getElementById('estado');
	const lista = document.getElementById('lista');
	const mensagem = document.getElementById('mensagem');
	const novo = document.getElementById('conteudo').dataset.novo;
	const ETAPAS = [['PENDENTE', 'Pendente'], ['EM_PREPARO', 'Em preparo'], ['PRONTO', 'Pronto'], ['ENTREGUE', 'Entregue']];

	function carregar() {
		mostrarEstado(estado, 'Carregando pedidos...', 'carregando');
		lista.hidden = true;
		requisitar('apiUser/pedidos')
			.then(({ ok, corpo }) => {
				if (!ok || !Array.isArray(corpo)) throw new Error('resposta inesperada');
				renderizar(corpo);
			})
			.catch(() => mostrarEstado(estado, 'Não foi possível carregar seus pedidos.', 'erro', { texto: 'Tentar novamente', fn: carregar }));
	}

	function renderizar(pedidos) {
		lista.replaceChildren();
		if (pedidos.length === 0) {
			lista.hidden = true;
			estado.replaceChildren();
			estado.className = 'estado';
			estado.hidden = false;
			estado.appendChild(document.createTextNode('Você ainda não fez nenhum pedido.'));
			estado.appendChild(document.createElement('br'));
			estado.appendChild(criar('a', { classe: 'btn', texto: 'Ver produtos', attrs: { href: 'home.jsp' } }));
			return;
		}
		mostrarEstado(estado, '', 'vazio');
		pedidos.forEach(p => lista.appendChild(criarCartao(p)));
		lista.hidden = false;
	}

	function criarCartao(p) {
		const itens = criar('ul', { classe: 'pedido-itens' });
		p.itens.forEach(i => itens.appendChild(criar('li', {
			texto: i.quantidade + ' × ' + i.nome + ' — ' + formatarPreco(i.subtotal)
		})));

		const cartao = criar('article', { classe: 'pedido-card' + (String(p.id) === novo ? ' destaque' : '') }, [
			criar('div', { classe: 'pedido-topo' }, [
				criar('h2', { texto: 'Pedido #' + p.id }),
				criar('span', { classe: 'status status-' + p.status, texto: p.statusRotulo })
			]),
			criar('p', { classe: 'pedido-meta', texto: formatarData(p.criadoEm) + (p.vendedorNome ? ' · ' + p.vendedorNome : '') }),
			itens,
			criar('p', { classe: 'pedido-total', texto: 'Total: ' + formatarPreco(p.total) })
		]);

		if (p.status !== 'CANCELADO') {
			const atual = ETAPAS.findIndex(e => e[0] === p.status);
			const passos = criar('ol', { classe: 'passos', attrs: { 'aria-label': 'Andamento do pedido' } });
			ETAPAS.forEach((e, idx) => passos.appendChild(criar('li', {
				classe: idx <= atual ? 'feito' : '', texto: e[1],
				attrs: idx === atual ? { 'aria-current': 'step' } : {}
			})));
			cartao.appendChild(passos);
		}
		if (p.status === 'CANCELADO') {
			if (p.canceladoPor === 'VENDEDOR') {
				cartao.appendChild(criar('div', { classe: 'alerta alerta-erro pedido-motivo' }, [
					criar('p', { texto: 'Cancelado pela cantina. Motivo:' }),
					criar('p', { classe: 'motivo-texto', texto: p.motivoCancelamento || '(não informado)' })
				]));
			} else if (p.canceladoPor === 'COMPRADOR') {
				cartao.appendChild(criar('p', { classe: 'pedido-cancelamento', texto: 'Você cancelou este pedido.' }));
			}
		}
				if (p.status === 'PRONTO') {
			cartao.appendChild(criar('p', { classe: 'alerta alerta-ok', texto: 'Seu pedido está pronto! Retire no balcão.' }));
		}
		if (p.podeCancelar) {
			const botao = criar('button', { classe: 'btn btn-perigo btn-pequeno', texto: 'Cancelar pedido', attrs: { type: 'button' } });
			botao.addEventListener('click', () => cancelar(p, botao));
			cartao.appendChild(criar('div', { classe: 'pedido-acoes' }, [botao]));
		}
		return cartao;
	}

	function cancelar(p, botao) {
		if (!confirm('Cancelar o pedido #' + p.id + '?')) return;
		botao.disabled = true;
		requisitar('apiUser/pedidos/cancelar', {
			method: 'POST',
			headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
			body: new URLSearchParams({ id: p.id }).toString()
		}).then(({ ok, corpo }) => {
			mostrarMensagem(mensagem, corpo.message || (ok ? 'Pedido cancelado.' : 'Não foi possível cancelar.'), !ok);
			carregar();
		}).catch(() => {
			mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true);
			botao.disabled = false;
		});
	}

	carregar();
});
