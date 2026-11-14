// "Meus pedidos": status e historico do comprador. Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	const estado = document.getElementById('estado');
	const lista = document.getElementById('lista');
	const novo = document.getElementById('conteudo').dataset.novo;

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
		ajustarRolagemDosItens(lista);
		confirmarPedidoNovo(pedidos);
	}

	// popup "pedido realizado" depois de finalizar a compra (?novo=<id>), so na primeira carga
	let confirmacaoMostrada = false;
	function confirmarPedidoNovo(pedidos) {
		if (confirmacaoMostrada || !novo) return;
		confirmacaoMostrada = true;
		const p = pedidos.find(x => String(x.id) === novo);
		if (p) notificar('Pedido #' + p.numero + ' realizado! Acompanhe o status abaixo e retire no balcão quando estiver pronto.');
	}

	function criarCartao(p) {
		const avisos = [];
		if (p.status === 'PRONTO') {
			avisos.push(criar('p', { classe: 'alerta alerta-ok pedido-aviso', texto: 'Seu pedido está pronto! Retire no balcão.' }));
		}
		if (p.status === 'CANCELADO' && p.canceladoPor === 'VENDEDOR') {
			avisos.push(criar('div', { classe: 'alerta alerta-erro pedido-aviso pedido-motivo' }, [
				criar('p', { texto: 'Cancelado pela cantina. Motivo:' }),
				criar('p', { classe: 'motivo-texto', texto: p.motivoCancelamento || '(não informado)' })
			]));
		} else if (p.status === 'CANCELADO' && p.canceladoPor === 'COMPRADOR') {
			avisos.push(criar('p', { classe: 'pedido-cancelamento', texto: 'Você cancelou este pedido.' }));
		}
		const acoes = [];
		if (p.podeCancelar) {
			const botao = criar('button', { classe: 'btn btn-perigo btn-pequeno', texto: 'Cancelar pedido', attrs: { type: 'button' } });
			botao.addEventListener('click', () => cancelar(p, botao));
			acoes.push(botao);
		}
		return cartaoPedido(p, {
			meta: formatarData(p.criadoEm) + (p.vendedorNome ? ' · ' + p.vendedorNome : ''),
			avisos: avisos,
			acoes: acoes,
			destaque: String(p.id) === novo
		});
	}

	function cancelar(p, botao) {
		if (!confirm('Cancelar o pedido #' + p.numero + '?')) return;
		botao.disabled = true;
		requisitar('apiUser/pedidos/cancelar', {
			method: 'POST',
			headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
			body: new URLSearchParams({ id: p.id }).toString()
		}).then(({ ok, corpo }) => {
			notificar(ok ? 'Pedido #' + p.numero + ' cancelado.' : (corpo.message || 'Não foi possível cancelar.'), ok ? 'ok' : 'erro');
			carregar();
		}).catch(() => {
			notificar('Erro de comunicação com o servidor.', 'erro');
			botao.disabled = false;
		});
	}

	carregar();
});
