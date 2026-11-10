// Tela do carrinho: lista os itens da sessao e mostra o total. Depende de util.js.
document.addEventListener('DOMContentLoaded', function() {
	const container = document.getElementById('carrinho-container');
	const total = document.getElementById('display-total');

	fetch('carrinho')
		.then(response => {
			if (!response.ok) {
				throw new Error('Resposta inesperada do servidor: ' + response.status);
			}
			return response.json();
		})
		.then(itens => {
			container.replaceChildren();
			let totalGeral = 0;
			if (!Array.isArray(itens) || itens.length === 0) {
				container.appendChild(criar('p', { texto: 'Seu carrinho está vazio.' }));
				total.textContent = formatarPreco(0);
				return;
			}
			itens.forEach(item => {
				totalGeral += item.preco * item.quantidade;
				const imagem = criar('img', { attrs: { src: urlImagem(item), alt: item.nome || 'Produto' } });
				imagem.style.cssText = 'width: 100px; height: 100px; object-fit: cover;';
				const detalhes = criar('div', {}, [
					criar('h3', { texto: item.nome }),
					criar('p', { texto: item.descricao || '' }),
					criar('p', { texto: 'Preço unitário: ' + formatarPreco(item.preco) }),
					criar('p', { texto: 'Quantidade: ' + item.quantidade }),
					criar('p', {}, [criar('strong', { texto: 'Subtotal: ' + formatarPreco(item.preco * item.quantidade) })])
				]);
				const linha = criar('div', {}, [imagem, detalhes]);
				linha.style.cssText = 'display: flex; gap: 10px; margin-bottom: 16px;';
				container.appendChild(criar('div', { classe: 'item-carrinho' }, [linha]));
			});
			total.textContent = formatarPreco(totalGeral);
		})
		.catch(error => {
			console.error('Erro ao carregar carrinho:', error);
			container.replaceChildren(criar('p', { texto: 'Erro ao carregar o carrinho.' }));
			total.textContent = formatarPreco(0);
		});
});
