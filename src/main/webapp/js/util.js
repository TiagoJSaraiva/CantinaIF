// Utilitarios compartilhados. Todas as URLs sao relativas ao contexto da aplicacao
// (as paginas ficam na raiz do contexto), entao nada depende de host, porta ou nome do WAR.

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function formatarPreco(valor) {
	return moeda.format(Number(valor) || 0);
}

// Cria um elemento; o texto sempre entra via textContent (nunca como HTML).
function criar(tag, opcoes = {}, filhos = []) {
	const e = document.createElement(tag);
	if (opcoes.classe) e.className = opcoes.classe;
	if (opcoes.texto !== undefined) e.textContent = opcoes.texto;
	if (opcoes.attrs) {
		Object.entries(opcoes.attrs).forEach(([k, v]) => e.setAttribute(k, v));
	}
	filhos.forEach(f => e.appendChild(f));
	return e;
}

function urlImagem(produto) {
	return produto.temImagem ? 'image?id=' + encodeURIComponent(produto.id) : 'img/placeholder.svg';
}

// Mostra uma mensagem no elemento indicado (sem alert()).
function mostrarMensagem(elemento, texto, erro) {
	elemento.textContent = texto;
	elemento.className = erro ? 'mensagem-erro' : 'mensagem-sucesso';
	elemento.style.display = texto ? 'block' : 'none';
}
