// Painel do vendedor - compradores: codigo de vinculo, tabela de compradores, desativar/reativar e advertencias.
// Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	const mensagem = document.getElementById('mensagem');

	// ---------- codigo de vinculo (60 s, uso unico) ----------
	const saidaCodigo = document.getElementById('codigo');
	const tempo = document.getElementById('codigo-tempo');
	const botaoGerar = document.getElementById('btn-gerar');
	let relogio = null;

	function pararRelogio() {
		if (relogio) clearInterval(relogio);
		relogio = null;
	}

	function mostrarCodigo(codigo, segundos) {
		pararRelogio();
		// o prazo e contado a partir da resposta (nao depende do relogio do computador estar certo)
		const fim = Date.now() + segundos * 1000;
		saidaCodigo.textContent = codigo;
		saidaCodigo.classList.remove('expirado');
		saidaCodigo.hidden = false;
		tempo.hidden = false;
		botaoGerar.textContent = 'Gerar outro código';
		const atualizar = () => {
			const restante = Math.max(0, Math.ceil((fim - Date.now()) / 1000));
			if (restante === 0) {
				pararRelogio();
				saidaCodigo.classList.add('expirado');
				tempo.textContent = 'Código expirado. Gere outro se precisar.';
				return;
			}
			tempo.textContent = 'Expira em ' + restante + ' s';
		};
		atualizar();
		relogio = setInterval(atualizar, 250);
	}

	botaoGerar.addEventListener('click', () => {
		botaoGerar.disabled = true;
		requisitar('api/vinculo/codigo', { method: 'POST', headers: cabecalhosCsrf() })
			.then(({ ok, corpo }) => {
				if (!ok || !corpo.codigo) throw new Error(corpo.message || 'erro');
				mostrarCodigo(corpo.codigo, Number(corpo.segundos) || 60);
				carregar(true); // quem acabou de se vincular aparece na tabela na proxima atualizacao
			})
			.catch(e => mostrarMensagem(mensagem, (e && e.message !== 'erro' && e.message) || 'Não foi possível gerar o código.', true))
			.finally(() => { botaoGerar.disabled = false; });
	});

	// ---------- tabela de compradores ----------
	const estado = document.getElementById('estado');
	const area = document.getElementById('area-tabela');
	const linhas = document.getElementById('linhas');
	const busca = document.getElementById('busca');
	let compradores = [];
	let situacao = 'todos';

	function carregar(silencioso) {
		if (!silencioso) {
			area.hidden = true;
			mostrarEstado(estado, 'Carregando compradores...', 'carregando');
		}
		requisitar('api/compradores')
			.then(({ ok, corpo }) => {
				if (!ok || !Array.isArray(corpo)) throw new Error('resposta inesperada');
				compradores = corpo;
				renderizar();
			})
			.catch(() => mostrarEstado(estado, 'Não foi possível carregar os compradores.', 'erro',
				{ texto: 'Tentar novamente', fn: () => carregar(false) }));
	}

	function renderizar() {
		const termo = semAcento(busca.value.trim());
		const visiveis = compradores.filter(c =>
			(situacao === 'todos' || (situacao === 'ativos') === c.ativo)
			&& (!termo || semAcento(c.nome + ' ' + c.prontuario + ' ' + c.email).includes(termo)));
		linhas.replaceChildren();
		if (compradores.length === 0) {
			area.hidden = true;
			mostrarEstado(estado, 'Nenhum aluno vinculado ainda. Gere um código de vínculo acima e mostre ao aluno.', 'vazio');
			return;
		}
		if (visiveis.length === 0) {
			area.hidden = true;
			mostrarEstado(estado, 'Nenhum comprador encontrado com esse filtro.', 'vazio');
			return;
		}
		mostrarEstado(estado, '', 'vazio');
		visiveis.forEach(c => linhas.appendChild(linha(c)));
		area.hidden = false;
	}

	function celula(rotulo, filhos, classe) {
		return criar('td', { classe: classe || '', attrs: { 'data-rotulo': rotulo } }, filhos);
	}

	function texto(t, classe) {
		return criar('span', { classe: classe || '', texto: String(t) });
	}

	function linha(c) {
		const advertir = criar('button', { classe: 'btn btn-secundario btn-pequeno', texto: 'Advertir', attrs: { type: 'button' } });
		advertir.addEventListener('click', () => abrirAdvertencia(c));
		const alternar = criar('button', {
			classe: c.ativo ? 'btn btn-perigo btn-pequeno' : 'btn btn-pequeno',
			texto: c.ativo ? 'Desativar' : 'Reativar',
			attrs: { type: 'button' }
		});
		alternar.addEventListener('click', () => mudarSituacao(c, !c.ativo, alternar));

		return criar('tr', { classe: c.ativo ? '' : 'inativo' }, [
			celula('Comprador', [
				texto(c.nome || c.prontuario, 'comprador-nome'),
				texto(c.prontuario + ' · ' + c.email, 'comprador-info')
			], 'celula-comprador'),
			celula('Pedidos', [texto(c.pedidos)], 'num'),
			celula('Cancelou', [texto(c.canceladosPeloComprador, c.canceladosPeloComprador >= 3 ? 'alerta-num' : '')], 'num'),
			celula('Cancelados por você', [texto(c.canceladosPeloVendedor)], 'num'),
			celula('Último pedido', [texto(c.ultimoPedido ? formatarData(c.ultimoPedido) : '—')]),
			celula('Advertências', [texto(c.advertencias)], 'num'),
			celula('Situação', [texto(c.ativo ? 'Ativo' : 'Desativado', 'situacao ' + (c.ativo ? 'situacao-ativo' : 'situacao-inativo'))]),
			celula('Ações', [criar('div', { classe: 'acoes-linha' }, [advertir, alternar])])
		]);
	}

	function mudarSituacao(c, ativar, botao) {
		const nome = c.nome || c.prontuario;
		const pergunta = ativar
			? 'Reativar ' + nome + '? Ele volta a ver seus produtos e a fazer pedidos.'
			: 'Desativar ' + nome + '? Ele perde o acesso a tudo (produtos, carrinho e pedidos) até você reativar.';
		if (!confirm(pergunta)) return;
		botao.disabled = true;
		requisitar('api/compradores/situacao', {
			method: 'POST',
			headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
			body: new URLSearchParams({ prontuario: c.prontuario, ativo: String(ativar) }).toString()
		}).then(({ ok, corpo }) => {
			mostrarMensagem(mensagem, corpo.message || (ok ? 'Situação atualizada.' : 'Não foi possível atualizar.'), !ok);
			carregar(true);
		}).catch(() => {
			mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true);
			botao.disabled = false;
		});
	}

	// ---------- advertencia ----------
	const dialogo = document.getElementById('dlg-advertencia');
	const form = document.getElementById('form-advertencia');
	const campo = document.getElementById('adv-mensagem');
	const contador = document.getElementById('adv-contador');
	const botaoEnviar = document.getElementById('btn-adv-enviar');
	let alvo = null;

	function abrirAdvertencia(c) {
		alvo = c;
		limparErros(form);
		campo.value = '';
		contador.textContent = '0';
		document.getElementById('dlg-adv-para').textContent = 'Para: ' + (c.nome || c.prontuario) + ' (' + c.prontuario + ')';
		dialogo.showModal();
		campo.focus();
	}

	document.querySelectorAll('.js-modelo').forEach(b => b.addEventListener('click', () => {
		campo.value = b.textContent.trim();
		contador.textContent = String(campo.value.length);
		marcarErro(campo, '');
		campo.focus();
	}));
	campo.addEventListener('input', () => { contador.textContent = String(campo.value.length); });
	document.getElementById('btn-adv-cancelar').addEventListener('click', () => dialogo.close());

	form.addEventListener('submit', evento => {
		evento.preventDefault();
		if (!alvo || !validarFormulario(form)) return;
		botaoEnviar.disabled = true;
		requisitar('api/compradores/advertencia', {
			method: 'POST',
			headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
			body: new URLSearchParams({ prontuario: alvo.prontuario, mensagem: campo.value.trim() }).toString()
		}).then(({ ok, corpo }) => {
			if (ok) {
				dialogo.close();
				mostrarMensagem(mensagem, corpo.message || 'Advertência enviada.', false);
				carregar(true);
			} else {
				marcarErro(campo, corpo.message || 'Não foi possível enviar.');
			}
		}).catch(() => marcarErro(campo, 'Erro de comunicação com o servidor.'))
			.finally(() => { botaoEnviar.disabled = false; });
	});

	document.querySelectorAll('#filtros-situacao .chip').forEach(b => b.addEventListener('click', () => {
		situacao = b.dataset.situacao;
		document.querySelectorAll('#filtros-situacao .chip').forEach(x => x.setAttribute('aria-pressed', String(x === b)));
		renderizar();
	}));
	busca.addEventListener('input', debounce(renderizar, 150));
	document.getElementById('btn-atualizar').addEventListener('click', () => carregar(false));
	carregar(false);
});
