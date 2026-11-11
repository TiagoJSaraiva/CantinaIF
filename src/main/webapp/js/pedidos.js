// Painel de pedidos do vendedor. Depende de util.js.
// Todo texto vindo do servidor entra no DOM via textContent (nunca como HTML).
document.addEventListener('DOMContentLoaded', function() {
	const pedidosContainer = document.getElementById('pedidosContainer');
	const loadingMessage = document.getElementById('loadingMessage');
	const errorMessage = document.getElementById('errorMessage');
	const noPedidosMessage = document.getElementById('noPedidosMessage');

	function criarCartaoPedido(pedido) {
		const card = criar('div', { classe: 'pedido-card' });
		const comprador = pedido.comprador || {};

		card.appendChild(criar('div', { classe: 'pedido-info' }, [
			criar('strong', { texto: 'Pedido #' + pedido.id }),
			document.createElement('br'),
			document.createTextNode('Comprador: '),
			criar('span', { texto: comprador.nome || '' }),
			document.createElement('br'),
			document.createTextNode('Prontuário: '),
			criar('span', { texto: comprador.prontuario || '' })
		]));

		card.appendChild(criar('h4', { texto: 'Itens do Pedido:' }));
		const lista = criar('ul');
		if (pedido.itens && pedido.itens.length > 0) {
			pedido.itens.forEach(item => {
				lista.appendChild(criar('li', { classe: 'pedido-item' }, [
					criar('strong', { texto: item.produto.nome }),
					document.createTextNode(' '),
					criar('span', { texto: '(Qtd: ' + item.quantidade + ', Preço Unit.: ' + formatarPreco(item.produto.preco) + ')' })
				]));
			});
		} else {
			lista.appendChild(criar('li', { classe: 'pedido-item', texto: 'Nenhum item neste pedido.' }));
		}
		card.appendChild(lista);

		const botao = criar('button', { classe: 'concluir-btn', texto: 'Concluir Pedido', attrs: { type: 'button' } });
		botao.addEventListener('click', () => {
			if (confirm('Tem certeza que deseja concluir o pedido #' + pedido.id + '? Esta ação é irreversível.')) {
				concluirPedido(pedido.id, botao);
			}
		});
		card.appendChild(botao);
		return card;
	}

	function fetchAndRenderPedidos() {
		loadingMessage.style.display = 'block';
		errorMessage.style.display = 'none';
		noPedidosMessage.style.display = 'none';
		pedidosContainer.querySelectorAll('.pedido-card').forEach(card => card.remove());

		fetch('api/pedidos')
			.then(response => {
				if (!response.ok) {
					throw new Error('Erro HTTP: ' + response.status);
				}
				return response.json();
			})
			.then(pedidos => {
				loadingMessage.style.display = 'none';
				if (!Array.isArray(pedidos) || pedidos.length === 0) {
					noPedidosMessage.style.display = 'block';
					return;
				}
				pedidos.forEach(pedido => pedidosContainer.appendChild(criarCartaoPedido(pedido)));
			})
			.catch(error => {
				loadingMessage.style.display = 'none';
				errorMessage.textContent = 'Erro ao carregar pedidos: ' + error.message;
				errorMessage.style.display = 'block';
			});
	}

	function concluirPedido(pedidoId, botao) {
		botao.disabled = true;
		botao.textContent = 'Concluindo...';

		fetch('api/deletarPedido', {
			method: 'POST',
			headers: cabecalhosCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
			body: new URLSearchParams({ id: pedidoId }).toString()
		})
			.then(response => response.json().catch(() => ({})).then(corpo => ({ ok: response.ok, corpo })))
			.then(({ ok, corpo }) => {
				if (ok && corpo.success) {
					mostrarMensagem(errorMessage, corpo.message, false);
					fetchAndRenderPedidos();
					errorMessage.style.display = 'block';
				} else {
					throw new Error(corpo.message || 'Erro ao concluir pedido.');
				}
			})
			.catch(error => {
				mostrarMensagem(errorMessage, error.message, true);
				botao.disabled = false;
				botao.textContent = 'Concluir Pedido';
			});
	}

	fetchAndRenderPedidos();
});
