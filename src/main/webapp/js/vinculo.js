// Formulario do codigo de vinculo com um vendedor (catalogo e "Minha conta"). Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('form.js-vinculo').forEach(form => {
		const campo = form.querySelector('input[name="codigo"]');
		const msg = form.querySelector('[data-vinculo-msg]');
		const botao = form.querySelector('button[type="submit"]');

		form.addEventListener('submit', evento => {
			evento.preventDefault();
			const codigo = campo.value.replace(/[\s-]/g, '').toUpperCase();
			if (codigo.length !== 6) {
				mostrarMensagem(msg, 'O código tem 6 caracteres.', true);
				campo.focus();
				return;
			}
			botao.disabled = true;
			requisitar('apiUser/vinculo', {
				method: 'POST',
				headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
				body: new URLSearchParams({ codigo: codigo }).toString()
			}).then(({ ok, corpo }) => {
				mostrarMensagem(msg, corpo.message || (ok ? 'Conta vinculada.' : 'Não foi possível vincular.'), !ok);
				if (ok) {
					// a pagina inteira depende do vinculo (catalogo, cabecalho): recarrega mostrando a confirmacao
					try { sessionStorage.setItem('vinculo-msg', corpo.message || ''); } catch (e) { /* sem armazenamento */ }
					setTimeout(() => window.location.reload(), 600);
				} else {
					botao.disabled = false;
					campo.select();
				}
			}).catch(() => {
				mostrarMensagem(msg, 'Erro de comunicação com o servidor.', true);
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
	const destino = document.getElementById('mensagem');
	if (pendente && destino) mostrarMensagem(destino, pendente, false);
});
