// Painel do vendedor - produtos: listar, criar, editar (inclui imagem) e ajustar estoque. Depende de util.js.

const ROTULO_CATEGORIA = { salgado: 'Salgado', lanche: 'Lanche', doce: 'Doce', bebida: 'Bebida' };
const TAMANHO_MAX_IMAGEM = 2 * 1024 * 1024;

const painel = { produtos: [], categoria: 'todos', busca: '' };

document.addEventListener('DOMContentLoaded', function() {
	const estado = document.getElementById('estado');
	const container = document.getElementById('productContainer');
	const mensagem = document.getElementById('mensagem');
	const dialogo = document.getElementById('dlg-produto');
	const form = document.getElementById('form-produto');
	const erroGeral = document.getElementById('erro-geral');

	// ---------- listagem ----------
	function carregar() {
		container.hidden = true;
		mostrarEstado(estado, 'Carregando produtos...', 'carregando');
		requisitar('apiAdm/produtos?categoria=' + encodeURIComponent(painel.categoria))
			.then(({ ok, corpo }) => {
				if (!ok || !Array.isArray(corpo)) throw new Error('resposta inesperada');
				painel.produtos = corpo;
				renderizar();
			})
			.catch(() => mostrarEstado(estado, 'Não foi possível carregar os produtos.', 'erro', { texto: 'Tentar novamente', fn: carregar }));
	}

	function renderizar() {
		const termo = semAcento(painel.busca.trim());
		const lista = painel.produtos.filter(p => !termo || semAcento(p.nome + ' ' + (p.descricao || '')).includes(termo));
		container.replaceChildren();
		if (painel.produtos.length === 0) {
			container.hidden = true;
			mostrarEstado(estado, 'Nenhum produto cadastrado nesta categoria.', 'vazio', { texto: 'Cadastrar produto', fn: () => abrirDialogo(null) });
			return;
		}
		if (lista.length === 0) {
			container.hidden = true;
			mostrarEstado(estado, 'Nenhum produto encontrado para "' + painel.busca.trim() + '".', 'vazio');
			return;
		}
		mostrarEstado(estado, '', 'vazio');
		lista.forEach(p => container.appendChild(criarCartao(p)));
		container.hidden = false;
	}

	function criarCartao(produto) {
		const esgotado = produto.estoque < 1;
		const cartao = criar('article', { classe: 'produto' + (esgotado ? ' esgotado' : '') });

		const areaImagem = criar('div', { classe: 'produto-img' }, [
			criar('img', { attrs: { src: urlImagem(produto), alt: produto.nome || 'Produto', loading: 'lazy' } })
		]);
		if (esgotado) areaImagem.appendChild(criar('span', { classe: 'selo selo-esgotado', texto: 'Esgotado' }));
		cartao.appendChild(areaImagem);

		const campoEstoque = criar('input', {
			attrs: { type: 'number', min: '0', value: String(produto.estoque), inputmode: 'numeric', 'aria-label': 'Estoque de ' + produto.nome }
		});
		const salvarEstoque = valor => {
			if (!Number.isInteger(valor) || valor < 0) { campoEstoque.value = String(produto.estoque); return; }
			requisitar('atualizarEstoque', {
				method: 'POST',
				headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
				body: new URLSearchParams({ id: produto.id, estoque: valor, esperado: produto.estoque }).toString()
			}).then(({ ok, status, corpo }) => {
				mostrarMensagem(mensagem, ok ? 'Estoque de ' + produto.nome + ' atualizado para ' + valor + '.' : (corpo.message || 'Erro ao atualizar o estoque.'), !ok);
				if (ok) { produto.estoque = valor; carregar(); } else { campoEstoque.value = String(produto.estoque); if (status === 409) carregar(); }
			}).catch(() => mostrarMensagem(mensagem, 'Erro de comunicação com o servidor.', true));
		};
		const menos = criar('button', { texto: '−', attrs: { type: 'button', 'aria-label': 'Diminuir estoque de ' + produto.nome } });
		const mais = criar('button', { texto: '+', attrs: { type: 'button', 'aria-label': 'Aumentar estoque de ' + produto.nome } });
		menos.addEventListener('click', () => salvarEstoque(produto.estoque - 1));
		mais.addEventListener('click', () => salvarEstoque(produto.estoque + 1));
		campoEstoque.addEventListener('change', () => salvarEstoque(parseInt(campoEstoque.value, 10)));

		const editar = criar('button', { classe: 'btn btn-secundario btn-pequeno', texto: 'Editar', attrs: { type: 'button', 'aria-label': 'Editar ' + produto.nome } });
		editar.addEventListener('click', () => abrirDialogo(produto));

		cartao.appendChild(criar('div', { classe: 'produto-corpo' }, [
			criar('span', { classe: 'categoria-rotulo', texto: ROTULO_CATEGORIA[produto.categoria] || '' }),
			criar('h3', { classe: 'produto-nome', texto: produto.nome }),
			criar('p', { classe: 'produto-desc', texto: produto.descricao || '' }),
			criar('span', { classe: 'preco', texto: formatarPreco(produto.preco) }),
			criar('div', { classe: 'produto-acoes' }, [
				criar('div', { classe: 'qtd' }, [menos, campoEstoque, mais]),
				editar
			])
		]));
		return cartao;
	}

	// ---------- dialogo criar/editar ----------
	function abrirDialogo(produto) {
		limparErros(form);
		form.reset();
		document.getElementById('dlg-titulo').textContent = produto ? 'Editar produto' : 'Novo produto';
		document.getElementById('produto-id').value = produto ? produto.id : '';
		if (produto) {
			document.getElementById('produto-nome').value = produto.nome || '';
			document.getElementById('produto-descricao').value = produto.descricao || '';
			document.getElementById('produto-preco').value = Number(produto.preco).toFixed(2);
			document.getElementById('produto-estoque').value = String(produto.estoque);
			document.getElementById('produto-estoque').dataset.original = String(produto.estoque);
			document.getElementById('produto-categoria').value = produto.categoria || '';
		}
		document.getElementById('dica-imagem').hidden = !produto;
		dialogo.showModal();
		document.getElementById('produto-nome').focus();
	}

	document.getElementById('btn-novo').addEventListener('click', () => abrirDialogo(null));
	document.getElementById('btn-cancelar-dlg').addEventListener('click', () => dialogo.close());

	document.getElementById('produto-imagem').addEventListener('change', e => {
		const arquivo = e.target.files[0];
		e.target.setCustomValidity(arquivo && arquivo.size > TAMANHO_MAX_IMAGEM ? 'A imagem deve ter no máximo 2 MB.' : '');
		marcarErro(e.target, e.target.validationMessage);
	});

	form.addEventListener('submit', evento => {
		evento.preventDefault();
		if (!validarFormulario(form)) return;
		const id = document.getElementById('produto-id').value;
		const dados = new FormData(form);
		if (!id) dados.delete('id');
		if (id) dados.set('estoqueAnterior', document.getElementById('produto-estoque').dataset.original || '');
		const botao = document.getElementById('btn-salvar');
		botao.disabled = true;
		erroGeral.hidden = true;

		requisitar(id ? 'editar-produto' : 'registrar-produto', { method: 'POST', headers: cabecalhosCsrf(), body: dados })
			.then(({ ok, corpo }) => {
				if (ok) {
					dialogo.close();
					mostrarMensagem(mensagem, corpo.message || 'Produto salvo.', false);
					carregar();
				} else if (corpo.erros) {
					mostrarErrosServidor(form, corpo.erros);
				} else {
					erroGeral.textContent = corpo.message || 'Não foi possível salvar o produto.';
					erroGeral.hidden = false;
				}
			})
			.catch(() => {
				erroGeral.textContent = 'Erro de comunicação com o servidor.';
				erroGeral.hidden = false;
			})
			.finally(() => { botao.disabled = false; });
	});

	// ---------- filtros ----------
	document.querySelectorAll('.chip[data-filter]').forEach(botao => {
		botao.addEventListener('click', () => {
			document.querySelectorAll('.chip[data-filter]').forEach(b => b.setAttribute('aria-pressed', String(b === botao)));
			painel.categoria = botao.dataset.filter;
			carregar();
		});
	});
	document.getElementById('busca').addEventListener('input', debounce(e => {
		painel.busca = e.target.value;
		if (painel.produtos.length) renderizar();
	}, 150));

	carregar();
});
