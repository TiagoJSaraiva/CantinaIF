// Tema claro/escuro. Carregado no <head> (sincrono) para aplicar a escolha salva antes de a pagina ser pintada.
// Sem escolha salva, o CSS segue o tema do sistema (prefers-color-scheme); o botao .js-tema alterna e lembra.
(function() {
	var raiz = document.documentElement;
	var salvo = null;
	try { salvo = localStorage.getItem('tema'); } catch (e) { /* armazenamento indisponivel */ }
	if (salvo === 'escuro' || salvo === 'claro') raiz.setAttribute('data-tema', salvo);

	var sistemaEscuro = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

	function escuroAtivo() {
		var escolhido = raiz.getAttribute('data-tema');
		if (escolhido) return escolhido === 'escuro';
		return !!(sistemaEscuro && sistemaEscuro.matches);
	}

	function atualizarBotoes() {
		var escuro = escuroAtivo();
		document.querySelectorAll('.js-tema').forEach(function(b) {
			b.setAttribute('aria-pressed', String(escuro));
			b.title = escuro ? 'Mudar para o modo claro' : 'Mudar para o modo escuro';
		});
	}

	document.addEventListener('DOMContentLoaded', function() {
		atualizarBotoes();
		document.querySelectorAll('.js-tema').forEach(function(b) {
			b.addEventListener('click', function() {
				var novo = escuroAtivo() ? 'claro' : 'escuro';
				raiz.setAttribute('data-tema', novo);
				try { localStorage.setItem('tema', novo); } catch (e) { /* so nesta pagina */ }
				atualizarBotoes();
			});
		});
		if (sistemaEscuro && sistemaEscuro.addEventListener) sistemaEscuro.addEventListener('change', atualizarBotoes);
	});
})();
