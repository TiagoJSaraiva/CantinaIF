<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Carrinho de Compras</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
        }
        .carrinho-container {
            width: auto;
            margin: 5%;
            border: 1px solid #aaa;
            padding: 20px;
            border-radius: 8px;
        }
        h2 {
            text-align: center;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 10px;
            text-align: left;
        }
        th {
            background-color: #f0f0f0;
        }
        .finalizar {
            text-align: center;
        }
        .finalizar button {
            padding: 10px 20px;
            font-size: 16px;
            background-color: #4CAF50;
            color: white;
            border: none;
            cursor: pointer;
            border-radius: 15px;
        }
        .finalizar button:hover {
            background-color: #45a049;
        }
        .item-carrinho {
      display: flex;
      align-items: flex-start;
      border-bottom: 1px solid #ccc;
      padding: 10px 0;
    }

    .item-carrinho img {
      width: 100px;
      height: auto;
      margin-right: 15px;
    }

    .info-produto {
      flex: 1;
    }

    .resumo {
      margin-top: 20px;
      font-weight: bold;
      font-size: 18px;
      text-align: right;
    }
    </style>
    <meta charset="UTF-8">
    <title>Carrinho - Cantina Universitária</title>
    <link rel="stylesheet" href="css/user.css"> <!-- Link do CSS -->
</head>
<body>

<!-- Barra superior com logo, prontuário do usuário e links -->
<div class="navbar">
    <div class="logo">
        	<a href="home.jsp"><img src="img/Logo.png" alt="Logo" style="height: 40px;"></a>
    </div>    
    <div class="info">
        <!-- Mostra o prontuário do comprador salvo na sessão -->
        <span>Prontuário: ${sessionScope.comprador.prontuario}</span>
        <a href="Carrinho.jsp" style="
        font-weight: bold;
		text-decoration: none;
  		color: white;">Carrinho</a>
        <a href="login.jsp" style="
        font-weight: bold;
		text-decoration: none;
  		color: white;">Logout</a>
    </div>
</div>

    <h1>Seu Carrinho</h1>
  <div id="carrinho-container"></div>
  <div class="resumo" id="resumo-total"></div>

        <div class="finalizar">
            <p><strong>Total: R$ </strong></p>
            <form action="FinalizarCompraServlet" method="post">
                <button type="submit">FINALIZAR COMPRA</button>
            </form>
        </div>
        
        <script>
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
  </script>
</body>
</html>
