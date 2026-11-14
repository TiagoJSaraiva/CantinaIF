// Advertencias recebidas pelo comprador ("Minha conta" e aviso de conta desativada). Depende de util.js.
// Marca como lidas quando o botao existe (conta ativa); na conta desativada so lista.
document.addEventListener('DOMContentLoaded', function() {
	const estado = document.getElementById('estado-adv');
	const lista = document.getElementById('lista-adv');
	const botaoLidas = document.getElementById('btn-lidas');
	if (!estado || !lista) return;

	function carregar() {
		mostrarEstado(estado, 'Carregando...', 'carregando');
		requisitar('apiUser/advertencias')
			.then(({ ok, corpo }) => {
				if (!ok || !Array.isArray(corpo.advertencias)) throw new Error('resposta inesperada');
				renderizar(corpo.advertencias, corpo.naoLidas || 0);
			})
			.catch(() => mostrarEstado(estado, 'Não foi possível carregar as advertências.', 'erro', { texto: 'Tentar novamente', fn: carregar }));
	}

	function renderizar(itens, naoLidas) {
		lista.replaceChildren();
		if (itens.length === 0) {
			lista.hidden = true;
			mostrarEstado(estado, 'Nenhuma advertência. Continue assim!', 'vazio');
			if (botaoLidas) botaoLidas.hidden = true;
			return;
		}
		mostrarEstado(estado, '', 'vazio');
		itens.forEach(a => {
			const item = criar('li', { classe: 'advertencia' + (a.lida ? '' : ' nao-lida') }, [
				criar('p', { classe: 'advertencia-meta' }, [
					criar('span', { classe: 'advertencia-cantina', texto: a.vendedorNome || 'Cantina' }),
					criar('time', { classe: 'advertencia-data', texto: formatarData(a.criadaEm), attrs: { datetime: a.criadaEm } })
				].concat(a.lida ? [] : [criar('span', { classe: 'sr-only', texto: '(nova)' })])),
				criar('p', { classe: 'advertencia-texto', texto: a.mensagem })
			]);
			lista.appendChild(item);
		});
		lista.hidden = false;
		if (botaoLidas) botaoLidas.hidden = naoLidas === 0;
	}

	if (botaoLidas) {
		botaoLidas.addEventListener('click', () => {
			botaoLidas.disabled = true;
			requisitar('apiUser/advertencias/lidas', { method: 'POST', headers: cabecalhosCsrf() })
				.then(({ ok }) => {
					if (ok) {
						document.querySelectorAll('.badge-avisos').forEach(b => b.remove());
						carregar();
					}
				})
				.finally(() => { botaoLidas.disabled = false; });
		});
	}
	carregar();
});
