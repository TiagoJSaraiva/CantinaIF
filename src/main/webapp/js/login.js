// Login e cadastro: preenche os campos com as contas de demonstracao (quando exibidas).
document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('.js-demo').forEach(botao => {
		botao.addEventListener('click', () => {
			document.getElementById('prontuario').value = botao.dataset.prontuario;
			document.getElementById('senha').value = botao.dataset.senha;
			document.querySelector('form[data-validar] button[type="submit"]').focus();
		});
	});
});
