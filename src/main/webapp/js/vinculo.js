// Formulario do codigo de vinculo com um vendedor (catalogo e "Minha conta"). Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('form.js-vinculo').forEach(form => {
		const campo = form.querySelector('input[name="codigo"]');
		const botao = form.querySelector('button[type="submit"]');

		form.addEventListener('submit', evento => {
			evento.preventDefault();
			const codigo = campo.value.replace(/[\s-]/g, '').toUpperCase();
			if (codigo.length !== 6) {
				marcarErro(campo, 'O código tem 6 caracteres.');
				campo.focus();
				return;
			}
			marcarErro(campo, '');
			botao.disabled = true;
			requisitar('apiUser/vinculo', {
				method: 'POST',
				headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
				body: new URLSearchParams({ codigo: codigo }).toString()
			}).then(({ ok, corpo }) => {
				if (ok) {
					// a pagina inteira depende do vinculo (catalogo, cabecalho): recarrega e mostra a confirmacao em popup
					try { sessionStorage.setItem('vinculo-msg', corpo.message || 'Conta vinculada.'); } catch (e) { /* sem armazenamento */ }
					window.location.reload();
				} else {
					marcarErro(campo, corpo.message || 'Não foi possível vincular.');
					botao.disabled = false;
					campo.select();
				}
			}).catch(() => {
				notificar('Erro de comunicação com o servidor.', 'erro');
				botao.disabled = false;
			});
		});
	});

	// confirmacao do vinculo feito antes do recarregamento
	let pendente = null;
	try {
		pendente = sessionStorage.getItem('vinculo-msg');
		sessionStorage.removeItem('vinculo-msg');
	} catch (e) { /* sem armazenamento */ }
	if (pendente) notificar(pendente);
});
