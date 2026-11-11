// Painel de pedidos do vendedor: agrupados/filtraveis por status, com acoes explicitas de avanco.
// Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	const estado = document.getElementById('estado');
	const lista = document.getElementById('lista');
	const mensagem = document.getElementById('mensagem');
	const filtros = document.getElementById('filtros-status');

	const ANDAMENTO = ['PENDENTE', 'EM_PREPARO', 'PRONTO'];
	const GRUPOS = [
		{ id: 'andamento', rotulo: 'Em andamento', status: ANDAMENTO },
		{ id: 'PENDENTE', rotulo: 'Pendentes', status: ['PENDENTE'] },
		{ id: 'EM_PREPARO', rotulo: 'Em preparo', status: ['EM_PREPARO'] },
		{ id: 'PRONTO', rotulo: 'Prontos', status: ['PRONTO'] },
		{ id: 'ENTREGUE', rotulo: 'Entregues', status: ['ENTREGUE'] },
		{ id: 'CANCELADO', rotulo: 'Cancelados', status: ['CANCELADO'] },
		{ id: 'todos', rotulo: 'Todos', status: null }
	];
	const ACAO = {
		EM_PREPARO: { texto: 'Iniciar preparo', classe: 'btn btn-pequeno' },
		PRONTO: { texto: 'Marcar como pronto', classe: 'btn btn-pequeno' },
		ENTREGUE: { texto: 'Marcar como entregue', classe: 'btn btn-pequeno' },
		CANCELADO: { texto: 'Cancelar pedido', classe: 'btn btn-perigo btn-pequeno' }
	};

	let pedidos = [];
	let grupoAtual = 'andamento';

	function pertence(p, grupo) {
		return !grupo.status || grupo.status.includes(p.status);
	}

	function carregar(silencioso) {
		if (!silencioso) {
			lista.hidden = true;
			mostrarEstado(estado, 'Carregando pedidos...', 'carregando');
		}
		requisitar('api/pedidos')
			.then(({ ok, corpo }) => {
				if (!ok || !Array.isArray(corpo)) throw new Error('resposta inesperada');
				pedidos = corpo;
				desenharFiltros();
				renderizar();
			})
			.catch(() => {
				if (!silencioso) mostrarEstado(estado, 'Não foi possível carregar os pedidos.', 'erro', { texto: 'Tentar novamente', fn: () => carregar(false) });
			});
	}

	function desenharFiltros() {
		filtros.replaceChildren();
		GRUPOS.forEach(g => {
			const n = pedidos.filter(p => pertence(p, g)).length;
			const botao = criar('button', { classe: 'chip', attrs: { type: 'button', 'aria-pressed': String(g.id === grupoAtual) } }, [
				document.createTextNode(g.rotulo + ' '),
				criar('span', { classe: 'contagem', texto: '(' + n + ')' })
			]);
			botao.addEventListener('click', () => { grupoAtual = g.id; desenharFiltros(); renderizar(); });
			filtros.appendChild(botao);
		});
	}

	function renderizar() {
		const grupo = GRUPOS.find(g => g.id === grupoAtual);
		const visiveis = pedidos.filter(p => pertence(p, grupo));
		lista.replaceChildren();
		if (visiveis.length === 0) {
			lista.hidden = true;
			mostrarEstado(estado, 'Nenhum pedido em "' + grupo.rotulo.toLowerCase() + '".', 'vazio');
			return;
		}
		mostrarEstado(estado, '', 'vazio');
		visiveis.forEach(p => lista.appendChild(criarCartao(p)));
		lista.hidden = false;
	}

	function criarCartao(p) {
		const itens = criar('ul', { classe: 'pedido-itens' });
		p.itens.forEach(i => itens.appendChild(criar('li', {
			texto: i.quantidade + ' × ' + i.nome + ' (' + formatarPreco(i.precoUnitario) + ')'
		})));
		const c = p.comprador || {};
		const cartao = criar('article', { classe: 'pedido-card' }, [
			criar('div', { classe: 'pedido-topo' }, [
				criar('h2', { texto: 'Pedido #' + p.id }),
				criar('span', { classe: 'status status-' + p.status, texto: p.statusRotulo })
			]),
			criar('p', { classe: 'pedido-meta', texto: formatarData(p.criadoEm) + ' · ' + (c.nome || '') + ' (' + (c.prontuario || '') + ')' }),
			itens,
			criar('p', { classe: 'pedido-total', texto: 'Total: ' + formatarPreco(p.total) })
		]);
		if (p.proximos.length) {
			const acoes = criar('div', { classe: 'pedido-acoes' });
			p.proximos.forEach(novo => {
				const def = ACAO[novo];
				const botao = criar('button', { classe: def.classe, texto: def.texto, attrs: { type: 'button' } });
				botao.addEventListener('click', () => mudarStatus(p, novo, botao));
				acoes.appendChild(botao);
			});
			cartao.appendChild(acoes);
		}
		return cartao;
	}

	function mudarStatus(p, novo, botao) {
		if (novo === 'CANCELADO' && !confirm('Cancelar o pedido #' + p.id + '? O estoque será devolvido.')) return;
		botao.disabled = true;
		requisitar('api/pedidos/status', {
			method: 'POST',
			headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
			body: new URLSearchParams({ id: p.id, status: novo }).toString()
		}).then(({ ok, corpo }) => {
			mostrarMensagem(mensagem, corpo.message || (ok ? 'Pedido atualizado.' : 'Não foi possível atualizar o pedido.'), !ok);
			carregar(true);
		}).catch(() => {
			mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true);
			botao.disabled = false;
		});
	}

	document.getElementById('btn-atualizar').addEventListener('click', () => carregar(false));
	// atualizacao periodica enquanto a aba esta visivel
	setInterval(() => { if (!document.hidden) carregar(true); }, 30000);

	carregar(false);
});
