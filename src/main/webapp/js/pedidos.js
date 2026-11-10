function toggleNewProductForm() {
	alert('Funcionalidade "Novo Produto" a ser implementada/conectada. Redirecionando para registroProdutos.jsp');
	window.location.href = "registroProdutos.jsp";
}

document.addEventListener('DOMContentLoaded', function() {
	const pedidosContainer = document.getElementById('pedidosContainer');
	const loadingMessage = document.getElementById('loadingMessage');
	const errorMessage = document.getElementById('errorMessage');
	const noPedidosMessage = document.getElementById('noPedidosMessage');

	// Função principal para buscar e renderizar os pedidos
	function fetchAndRenderPedidos() {
		// Reseta as mensagens e o container
		loadingMessage.style.display = 'block';
		errorMessage.style.display = 'none';
		noPedidosMessage.style.display = 'none';
		pedidosContainer.querySelectorAll('.pedido-card').forEach(card => card.remove()); // Remove cards antigos

		fetch("api/pedidos") // Chama o PedidosJsonServlet
			.then(response => {
				if (!response.ok) {
					// Se a resposta não for OK, tenta ler o erro do servidor ou gera um erro genérico
					return response.text().then(text => { // Pega o texto da resposta para depuração
						throw new Error(`Erro HTTP: ${response.status} - ${response.statusText} - Detalhe: ${text}`);
					}).catch(() => {
						throw new Error(`Erro HTTP: ${response.status} - ${response.statusText}`);
					});
				}
				return response.json(); // Converte a resposta para JSON
			})
			.then(pedidos => {
				loadingMessage.style.display = 'none'; // Esconde mensagem de carregamento


				if (!Array.isArray(pedidos) || pedidos.length === 0) {
					noPedidosMessage.style.display = 'block'; // Mostra mensagem de "nenhum pedido"
					return;
				}

				// Itera sobre a lista de pedidos e cria os cards HTML
				pedidos.forEach(pedido => {
					// console.log para depurar o objeto Pedido e suas propriedades (manter para depuração)
					console.log("Processando Pedido:", pedido);
					console.log("Objeto Comprador:", pedido.comprador);
					console.log("Nome do Comprador (acessado):", pedido.comprador ? pedido.comprador.nome : "Comprador objeto nulo");
					console.log("Prontuário do Comprador (acessado):", pedido.comprador ? pedido.comprador.prontuario : "Comprador objeto nulo");


					const pedidoCard = document.createElement('div');
					pedidoCard.classList.add('pedido-card');

					// --- CORREÇÕES AQUI: RE-INSERINDO AS VARIÁVEIS NA STRING HTML ---
					let compradorHtml = `
                           <div class="pedido-info">
                               <strong>Pedido #${pedido.id}</strong><br>
                               Comprador: <span>${pedido.comprador.nome}</span><br>
                               Prontuário: <span>${pedido.comprador.prontuario}</span>
                           </div>
                       `;

					// Lista de itens do pedido
					let itensHtml = '<h4>Itens do Pedido:</h4><ul>';
					if (pedido.itens && pedido.itens.length > 0) {
						pedido.itens.forEach(item => {
							// Garante que o preço seja formatado corretamente
							// Usa parseFloat().toFixed(2) para garantir 2 casas decimais, mesmo que o float venha sem.
							const precoUnitario = item.produto.preco ? parseFloat(item.produto.preco).toFixed(2) : '0.00';
							itensHtml += `
                                   <li class="pedido-item">
                                       <strong>${item.produto.nome}</strong> 
                                       <span>(Qtd: ${item.quantidade}, Preço Unit.: R$ ${precoUnitario})</span>
                                   </li>
                               `;
						});
					} else {
						itensHtml += '<li class="pedido-item">Nenhum item neste pedido.</li>';
					}
					itensHtml += '</ul>';

					// Botão de concluir pedido
					const concluirButton = `<button class="concluir-btn" data-pedido-id="${pedido.id}">Concluir Pedido</button>`;

					pedidoCard.innerHTML = compradorHtml + itensHtml + concluirButton;
					pedidosContainer.appendChild(pedidoCard);
				});

				// Adiciona os event listeners aos botões de concluir após a criação dos elementos
				addConcluirButtonListeners();

			})
			.catch(error => {
				loadingMessage.style.display = 'none';
				errorMessage.textContent = 'Erro ao carregar pedidos: ' + error.message;
				errorMessage.style.display = 'block';
				console.error('Erro ao buscar pedidos:', error);
			});
	}

	// Função para adicionar listeners aos botões "Concluir Pedido"
	function addConcluirButtonListeners() {
		document.querySelectorAll('.concluir-btn').forEach(button => {
			button.addEventListener('click', function() {
				const pedidoId = this.dataset.pedidoId;
				if (confirm(`Tem certeza que deseja concluir o pedido #${pedidoId}? Esta ação é irreversível.`)) {
					concluirPedido(pedidoId);
				}
			});
		});
	}

	// Função para enviar a requisição de conclusão de pedido via AJAX
	function concluirPedido(pedidoId) {
		const button = document.querySelector(`.concluir-btn[data-pedido-id="${pedidoId}"]`);
		if (button) {
			button.disabled = true;
			button.textContent = 'Concluindo...';
		}

		fetch("api/deletarPedido", { // Chama o DeletarPedidoServlet
			method: 'POST',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded',
			},
			body: `id=${pedidoId}`
		})
			.then(response => {
				if (!response.ok) {
					return response.json().then(err => {
						throw new Error(err.message || 'Erro desconhecido ao concluir pedido.');
					}).catch(() => {
						throw new Error(`Erro HTTP: ${response.status} - ${response.statusText}`);
					});
				}
				return response.json();
			})
			.then(data => {
				if (data.success) {
					alert(data.message);
					fetchAndRenderPedidos(); // Recarrega a lista
				} else {
					alert('Erro: ' + data.message);
					if (button) {
						button.disabled = false;
						button.textContent = 'Concluir Pedido';
					}
				}
			})
			.catch(error => {
				alert('Erro na comunicação com o servidor: ' + error.message);
				console.error('Erro na requisição AJAX para deletar pedido:', error);
				if (button) {
					button.disabled = false;
					button.textContent = 'Concluir Pedido';
				}
			});
	}

	// Inicia o carregamento dos pedidos quando a página é completamente carregada
	fetchAndRenderPedidos();
});
