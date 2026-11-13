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
		if (p.status === 'CANCELADO') {
			cartao.appendChild(criar('p', {
				classe: 'pedido-cancelamento',
				texto: p.canceladoPor === 'VENDEDOR'
					? 'Cancelado por você: ' + (p.motivoCancelamento || '')
					: 'Cancelado pelo aluno.'
			}));
		}
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
		if (novo === 'CANCELADO') {
			abrirCancelamento(p);
			return;
		}
		botao.disabled = true;
		enviarStatus(p, novo, null).catch(() => { botao.disabled = false; });
	}

	// resolve com { ok, corpo }; rejeita em erro de comunicacao (ja mostrado)
	function enviarStatus(p, novo, motivo) {
		const dados = { id: p.id, status: novo };
		if (motivo) dados.motivo = motivo;
		return requisitar('api/pedidos/status', {
			method: 'POST',
			headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
			body: new URLSearchParams(dados).toString()
		}).then(r => {
			if (r.ok || !motivo) {
				mostrarMensagem(mensagem, r.corpo.message || (r.ok ? 'Pedido atualizado.' : 'Não foi possível atualizar o pedido.'), !r.ok);
				carregar(true);
			}
			return r;
		}, e => {
			mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true);
			throw e;
		});
	}

	// ---------- cancelamento com motivo obrigatorio ----------
	const dialogo = document.getElementById('dlg-cancelar');
	const form = document.getElementById('form-cancelar');
	const campoMotivo = document.getElementById('motivo');
	const botaoConfirmar = document.getElementById('btn-cancelar-confirmar');
	let pedidoCancelando = null;

	function abrirCancelamento(p) {
		pedidoCancelando = p;
		limparErros(form);
		campoMotivo.value = '';
		const c = p.comprador || {};
		document.getElementById('dlg-cancelar-info').textContent =
			'Pedido #' + p.id + ' de ' + (c.nome || c.prontuario || '') + ' · ' + formatarPreco(p.total);
		dialogo.showModal();
		campoMotivo.focus();
	}
	document.querySelectorAll('.js-motivo').forEach(b => b.addEventListener('click', () => {
		campoMotivo.value = b.textContent.trim();
		marcarErro(campoMotivo, '');
		campoMotivo.focus();
	}));
	document.getElementById('btn-cancelar-voltar').addEventListener('click', () => dialogo.close());
	form.addEventListener('submit', evento => {
		evento.preventDefault();
		if (!pedidoCancelando || !validarFormulario(form)) return;
		botaoConfirmar.disabled = true;
		enviarStatus(pedidoCancelando, 'CANCELADO', campoMotivo.value.trim())
			.then(({ ok, corpo }) => {
				if (ok) dialogo.close();
				else marcarErro(campoMotivo, corpo.message || 'Não foi possível cancelar.');
			})
			.catch(() => dialogo.close())
			.finally(() => { botaoConfirmar.disabled = false; });
	});

	document.getElementById('btn-atualizar').addEventListener('click', () => carregar(false));
	// atualizacao periodica enquanto a aba esta visivel
	setInterval(() => { if (!document.hidden) carregar(true); }, 30000);

	carregar(false);
});
