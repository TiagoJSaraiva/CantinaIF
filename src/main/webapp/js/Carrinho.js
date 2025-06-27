document.addEventListener("DOMContentLoaded", function() {
	// --- ATENÇÃO: CONFIGURE O CAMINHO DO SEU CONTEXTO AQUI ---
	const CONTEXT_PATH = '/CantinaIF'; // <--- AJUSTE AQUI SE NECESSÁRIO

	fetch(`${CONTEXT_PATH}/carrinho`) // Chama o Servlet que retorna os itens do carrinho (GET por padrão)
		.then(response => {
			if (!response.ok) {
				return response.text().then(text => {
					throw new Error(`Erro HTTP: ${response.status} - ${response.statusText} - Detalhe: ${text}`);
				}).catch(() => {
					throw new Error(`Erro HTTP: ${response.status} - ${response.statusText}`);
				});
			}
			return response.json();
		})
		.then(data => {
			const container = document.getElementById("carrinho-container");
			const displayTotalSpan = document.getElementById("display-total"); // Captura a span para o total final

			container.innerHTML = ""; // Limpa o conteúdo atual do carrinho

			let totalGeral = 0;

			const itensCarrinho = data.itens || data; // Se o JSON direto for um array, usa-o. Se for um objeto com 'itens', usa 'data.itens'

			if (!itensCarrinho || itensCarrinho.length === 0) {
				container.innerHTML = '<p>Seu carrinho está vazio.</p>';
				if (displayTotalSpan) displayTotalSpan.textContent = '0.00'; // Zera o total principal
				return;
			}

			itensCarrinho.forEach(item => {
				const { produto, quantidade } = item;
				const { nome, descricao, preco } = produto;

				const imagem = produto.imagemBase64 ? `data:image/jpeg;base64,${produto.imagemBase64}` : "img/padrao.png";

				const subtotal = preco * quantidade;

				totalItens += quantidade;
				totalGeral += subtotal;

				const itemDiv = document.createElement("div");
				itemDiv.classList.add("item-carrinho");

				itemDiv.innerHTML = `
          <div style="display: flex; gap: 10px; margin-bottom: 16px;">
            <img src="${imagem}" alt="Produto" style="width: 100px; height: 100px; object-fit: cover;">
            <div>
              <h3>${nome}</h3>
              <p>${descricao || ''}</p>
              <p>Preço unitário: R$ ${preco.toFixed(2)}</p>
              <p>Quantidade: ${quantidade}</p>
              <p><strong>Total: R$ ${subtotal.toFixed(2)}</strong></p>
            </div>
          </div>
        `;
				container.appendChild(itemDiv);
			});

			// --- CORREÇÃO FINAL AQUI ---
			// Atualiza o total visível no HTML na span 'display-total'
			console.log("Total Geral Calculado:", totalGeral); // Adicionar para depuração
			if (displayTotalSpan) {
				displayTotalSpan.textContent = totalGeral.toFixed(2);
			}
			// REMOVIDA A LÓGICA DE ATUALIZAÇÃO DA div id="resumo-total"

		})
		.catch(error => {
			console.error("Erro ao carregar carrinho:", error);
			document.getElementById("carrinho-container").innerHTML = '<p>Erro ao carregar o carrinho.</p>';
			const displayTotalSpan = document.getElementById("display-total");
			if (displayTotalSpan) displayTotalSpan.textContent = '0.00';
		});
});
