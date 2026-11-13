// Login e cadastro: preenche os campos com as contas de demonstracao (quando exibidas) e, no cadastro,
// alterna os campos de aluno e de vendedor (os campos do perfil nao escolhido ficam desabilitados: nao sao
// validados nem enviados).
document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('.js-demo').forEach(botao => {
		botao.addEventListener('click', () => {
			document.getElementById('prontuario').value = botao.dataset.prontuario;
			document.getElementById('senha').value = botao.dataset.senha;
			document.querySelector('form[data-validar] button[type="submit"]').focus();
		});
	});

	const form = document.getElementById('form-cadastro');
	if (!form) return;
	function aplicarPerfil() {
		const marcado = form.querySelector('input[name="perfil"]:checked');
		const perfil = marcado ? marcado.value : 'aluno';
		form.querySelectorAll('.perfil-campos').forEach(grupo => {
			const ativo = grupo.dataset.perfil === perfil;
			grupo.hidden = !ativo;
			grupo.disabled = !ativo;
		});
	}
	form.querySelectorAll('input[name="perfil"]').forEach(r => r.addEventListener('change', aplicarPerfil));
	aplicarPerfil();
});
