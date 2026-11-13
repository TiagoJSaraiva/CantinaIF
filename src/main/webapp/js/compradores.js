// Painel do vendedor - compradores: codigo de vinculo. Depende de util.js.
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
			})
			.catch(e => mostrarMensagem(mensagem, (e && e.message !== 'erro' && e.message) || 'Não foi possível gerar o código.', true))
			.finally(() => { botaoGerar.disabled = false; });
	});
});
