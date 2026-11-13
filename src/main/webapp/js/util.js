// Utilitarios compartilhados. Todas as URLs sao relativas ao contexto da aplicacao
// (as paginas ficam na raiz do contexto), entao nada depende de host, porta ou nome do WAR.
// Todo texto vindo do servidor entra no DOM via textContent (nunca como HTML).

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const formatoData = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

function formatarPreco(valor) {
	return moeda.format(Number(valor) || 0);
}

function formatarData(iso) {
	const d = new Date(iso);
	return isNaN(d) ? '' : formatoData.format(d);
}

// Cria um elemento; o texto sempre entra via textContent.
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

// Token CSRF da sessao (renderizado pelo servidor em <meta name="csrf-token">).
function csrfToken() {
	const meta = document.querySelector('meta[name="csrf-token"]');
	return meta ? meta.content : '';
}

// Cabecalhos de requisicoes que alteram estado: inclui o token CSRF.
function cabecalhosCsrf(extra = {}) {
	return Object.assign({ 'X-CSRF-Token': csrfToken() }, extra);
}

// fetch que sempre resolve com { ok, status, corpo } (corpo = JSON ou {}).
function requisitar(url, opcoes) {
	return fetch(url, opcoes).then(resposta =>
		resposta.json().catch(() => ({})).then(corpo => {
			if (resposta.status === 401 && corpo.restaurada) {
				// a base de demonstracao foi restaurada: a sessao nao vale mais
				window.location.href = 'login.jsp?restaurada=1';
			}
			if (resposta.status === 403 && corpo.desativada && !/conta-desativada\.jsp$/.test(window.location.pathname)) {
				// o vendedor desativou esta conta: so resta ver o aviso
				window.location.href = 'conta-desativada.jsp';
			}
			return { ok: resposta.ok, status: resposta.status, corpo };
		}));
}

function urlImagem(produto) {
	return produto.temImagem ? 'image?id=' + encodeURIComponent(produto.id) : 'img/placeholder.svg';
}

function semAcento(texto) {
	return (texto || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

function debounce(fn, ms) {
	let t;
	return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

// Mensagem de sucesso/erro (regiao aria-live), sem alert().
function mostrarMensagem(elemento, texto, erro) {
	if (!elemento) return;
	elemento.textContent = texto || '';
	elemento.className = 'alerta ' + (erro ? 'alerta-erro' : 'alerta-ok');
	elemento.setAttribute('role', erro ? 'alert' : 'status');
	elemento.hidden = !texto;
}

// Area de estado (carregando / vazio / erro). Passe texto vazio para esconder.
function mostrarEstado(elemento, texto, tipo, acao) {
	elemento.replaceChildren();
	elemento.className = 'estado' + (tipo === 'carregando' ? ' carregando' : '');
	elemento.hidden = !texto;
	if (!texto) return;
	elemento.appendChild(document.createTextNode(texto));
	if (acao) {
		elemento.appendChild(document.createElement('br'));
		const botao = criar('button', { classe: 'btn btn-secundario', texto: acao.texto, attrs: { type: 'button' } });
		botao.addEventListener('click', acao.fn);
		elemento.appendChild(botao);
	}
}

// Atualiza o contador do carrinho no cabecalho.
function atualizarBadgeCarrinho() {
	const badge = document.getElementById('badge-carrinho');
	if (!badge) return;
	requisitar('carrinho').then(({ ok, corpo }) => {
		if (ok && Array.isArray(corpo)) {
			badge.textContent = String(corpo.reduce((soma, i) => soma + i.quantidade, 0));
		}
	}).catch(() => {});
}

// ---------- validacao de formularios (cliente) ----------
function mensagemDe(el) {
	const v = el.validity;
	if (el.validationMessage && v.customError) return el.validationMessage;
	if (v.valueMissing) return el.dataset.msgObrigatorio || 'Preencha este campo.';
	if (v.typeMismatch) return el.type === 'email' ? 'Informe um e-mail válido.' : 'Valor inválido.';
	if (v.patternMismatch) return el.dataset.msgPattern || 'Formato inválido.';
	if (v.tooShort) return 'Use pelo menos ' + el.minLength + ' caracteres.';
	if (v.tooLong) return 'Use no máximo ' + el.maxLength + ' caracteres.';
	if (v.rangeUnderflow) return 'O valor mínimo é ' + el.min + '.';
	if (v.rangeOverflow) return 'O valor máximo é ' + el.max + '.';
	if (v.stepMismatch) return 'Valor inválido (verifique as casas decimais).';
	if (v.badInput) return 'Informe um número válido.';
	return el.validationMessage || 'Valor inválido.';
}

function erroDoCampo(el) {
	return el.id ? document.getElementById('erro-' + (el.name || el.id)) : null;
}

function marcarErro(el, texto) {
	const alvo = erroDoCampo(el);
	if (alvo) {
		alvo.textContent = texto || '';
		if (texto) {
			el.setAttribute('aria-invalid', 'true');
			el.setAttribute('aria-describedby', alvo.id);
		} else {
			el.removeAttribute('aria-invalid');
			el.removeAttribute('aria-describedby');
		}
	}
}

function limparErros(form) {
	form.querySelectorAll('[name]').forEach(el => { el.setCustomValidity && el.setCustomValidity(''); marcarErro(el, ''); });
	const geral = form.querySelector('#erro-geral');
	if (geral) geral.hidden = true;
}

// Valida os campos, escreve as mensagens junto a cada campo e foca o primeiro invalido.
function validarFormulario(form) {
	let primeiro = null;
	form.querySelectorAll('input[name], select[name], textarea[name]').forEach(el => {
		if (el.type === 'hidden') return;
		if (el.checkValidity()) {
			marcarErro(el, '');
		} else {
			marcarErro(el, mensagemDe(el));
			if (!primeiro) primeiro = el;
		}
	});
	if (primeiro) primeiro.focus();
	return !primeiro;
}

// Mostra erros do servidor ({campo: mensagem}) junto aos campos.
function mostrarErrosServidor(form, erros) {
	let primeiro = null;
	Object.entries(erros || {}).forEach(([campo, texto]) => {
		const el = form.querySelector('[name="' + campo + '"]');
		if (el) {
			marcarErro(el, texto);
			if (!primeiro) primeiro = el;
		}
	});
	if (primeiro) primeiro.focus();
}

document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('form[data-validar]').forEach(form => {
		form.addEventListener('submit', evento => {
			if (!validarFormulario(form)) evento.preventDefault();
		});
		form.addEventListener('input', evento => {
			const el = evento.target;
			if (el.name && el.type !== 'hidden') {
				if (el.setCustomValidity) el.setCustomValidity('');
				marcarErro(el, '');
			}
		});
	});
});

// ---------- card de pedido (Meus pedidos e painel do vendedor) ----------
// Estrutura fixa: topo (numero + status), meta, itens numa area com rolagem propria e rodape sempre no fim do card
// (total, avisos, etapas e acoes). A lista usa linhas de altura igual, entao todos os cards ficam do mesmo tamanho.
const ETAPAS_PEDIDO = [['PENDENTE', 'Pendente'], ['EM_PREPARO', 'Em preparo'], ['PRONTO', 'Pronto'], ['ENTREGUE', 'Entregue']];

// opcoes: { meta: texto, avisos: [elementos], acoes: [botoes], destaque: bool }
function cartaoPedido(p, opcoes = {}) {
	const lista = criar('ul', { classe: 'pedido-itens' });
	p.itens.forEach(i => {
		const preco = [criar('span', { classe: 'item-subtotal', texto: formatarPreco(i.subtotal) })];
		if (i.quantidade > 1) preco.push(criar('span', { classe: 'item-unitario', texto: formatarPreco(i.precoUnitario) + ' cada' }));
		lista.appendChild(criar('li', { classe: 'pedido-item' }, [
			criar('span', { classe: 'item-nome', texto: i.quantidade + ' × ' + i.nome }),
			criar('span', { classe: 'item-preco' }, preco)
		]));
	});
	const qtdLinhas = p.itens.length;
	const areaItens = criar('section', { classe: 'pedido-itens-area', attrs: { 'aria-label': 'Itens do pedido #' + p.id } }, [
		criar('div', { classe: 'pedido-itens-cab' }, [
			criar('span', { texto: 'Itens do pedido' }),
			criar('span', { classe: 'pedido-itens-qtd', texto: qtdLinhas + (qtdLinhas === 1 ? ' item' : ' itens') })
		]),
		lista
	]);

	const rodape = criar('div', { classe: 'pedido-rodape' }, [
		criar('div', { classe: 'pedido-total-linha' }, [
			criar('span', { texto: 'Total' }),
			criar('strong', { classe: 'pedido-total-valor', texto: formatarPreco(p.total) })
		])
	]);
	(opcoes.avisos || []).forEach(a => rodape.appendChild(a));
	if (p.status !== 'CANCELADO') {
		const atual = ETAPAS_PEDIDO.findIndex(e => e[0] === p.status);
		const passos = criar('ol', { classe: 'passos', attrs: { 'aria-label': 'Andamento do pedido' } });
		ETAPAS_PEDIDO.forEach((e, idx) => passos.appendChild(criar('li', {
			classe: idx <= atual ? 'feito' : '', texto: e[1],
			attrs: idx === atual ? { 'aria-current': 'step' } : {}
		})));
		rodape.appendChild(passos);
	}
	if (opcoes.acoes && opcoes.acoes.length) rodape.appendChild(criar('div', { classe: 'pedido-acoes' }, opcoes.acoes));

	return criar('article', { classe: 'pedido-card' + (opcoes.destaque ? ' destaque' : '') }, [
		criar('div', { classe: 'pedido-topo' }, [
			criar('h2', { texto: 'Pedido #' + p.id }),
			criar('span', { classe: 'status status-' + p.status, texto: p.statusRotulo })
		]),
		criar('p', { classe: 'pedido-meta', texto: opcoes.meta || formatarData(p.criadoEm), attrs: { title: opcoes.meta || '' } }),
		areaItens,
		rodape
	]);
}

// Depois de os cards entrarem na pagina: listas de itens que transbordam ganham foco por teclado (para rolar).
function ajustarRolagemDosItens(container) {
	requestAnimationFrame(() => container.querySelectorAll('.pedido-itens').forEach(ul => {
		if (ul.scrollHeight > ul.clientHeight + 1) {
			ul.setAttribute('tabindex', '0');
			ul.classList.add('rolavel');
			// esmaecimento embaixo enquanto houver mais itens; some ao chegar ao fim
			const noFim = () => ul.classList.toggle('no-fim', ul.scrollTop + ul.clientHeight >= ul.scrollHeight - 2);
			ul.addEventListener('scroll', noFim, { passive: true });
			noFim();
		} else {
			ul.removeAttribute('tabindex');
			ul.classList.remove('rolavel');
		}
	}));
}
