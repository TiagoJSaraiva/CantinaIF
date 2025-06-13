fetch('carrinho')
      .then(res => res.json())
      .then(itens => {
        const container = document.getElementById('carrinho-container');
        const resumo = document.getElementById('resumo-total');
        let total = 0;
        let totalItens = 0;

        if (itens.length === 0) {
          container.innerHTML = '<p>Seu carrinho está vazio.</p>';
          resumo.innerHTML = '';
          return;
        }

        itens.forEach(item => {
          total += item.subtotal;
          totalItens += item.quantidade;

          const div = document.createElement('div');
          div.className = 'item-carrinho';

          div.innerHTML = `
            <img src="${item.imagemUrl}" alt="${item.nome}">
            <div class="info-produto">
              <h3>${item.nome}</h3>
              <p>${item.descricao}</p>
              <p>Preço unitário: R$ ${item.preco.toFixed(2)}</p>
              <p>Quantidade: ${item.quantidade}</p>
              <p><strong>Subtotal: R$ ${item.subtotal.toFixed(2)}</strong></p>
            </div>
          `;

          container.appendChild(div);
        });

        resumo.innerHTML = `
          Total de produtos: ${totalItens}<br>
          Valor total: <strong>R$ ${total.toFixed(2)}</strong>
        `;
      });