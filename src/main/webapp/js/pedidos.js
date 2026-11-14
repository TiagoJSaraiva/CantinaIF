// Painel de pedidos do vendedor: agrupados/filtraveis por status, com acoes explicitas de avanco.
// Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	const estado = document.getElementById('estado');
	const lista = document.getElementById('lista');
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
		PRONTO: { texto: 'Marcar pronto', classe: 'btn btn-pequeno' },
		ENTREGUE: { texto: 'Marcar entregue', classe: 'btn btn-pequeno' },
		CANCELADO: { texto: 'Cancelar', classe: 'btn btn-perigo btn-pequeno' }
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
		ajustarRolagemDosItens(lista);
	}

	function criarCartao(p) {
		const c = p.comprador || {};
		const avisos = [];
		if (p.status === 'CANCELADO') {
			avisos.push(p.canceladoPor === 'VENDEDOR'
				? criar('div', { classe: 'alerta alerta-erro pedido-aviso pedido-motivo' }, [
					criar('p', { texto: 'Cancelado por você. Motivo:' }),
					criar('p', { classe: 'motivo-texto', texto: p.motivoCancelamento || '(não informado)' })
				])
				: criar('p', { classe: 'pedido-cancelamento', texto: 'Cancelado pelo aluno.' }));
		}
		const acoes = p.proximos.map(novo => {
			const def = ACAO[novo];
			const botao = criar('button', { classe: def.classe, texto: def.texto, attrs: { type: 'button' } });
			botao.addEventListener('click', () => mudarStatus(p, novo, botao));
			return botao;
		});
		return cartaoPedido(p, {
			meta: formatarData(p.criadoEm) + ' · ' + (c.nome || '') + ' (' + (c.prontuario || '') + ')',
			avisos: avisos,
			acoes: acoes
		});
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
				notificar(r.corpo.message || (r.ok ? 'Pedido atualizado.' : 'Não foi possível atualizar o pedido.'), r.ok ? 'ok' : 'erro');
				carregar(true);
			}
			return r;
		}, e => {
			notificar('Erro de comunicação com o servidor.', 'erro');
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
