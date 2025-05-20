<%-- AVISO: Protótipo para fins de teste, deve ser aprimorado depois. --%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Registro de produtos</title>
    <link rel="stylesheet" href="css/login.css">
</head>
<body>
    <section class="login-page">
        <h1>Registro de produtos</h1>
        <form class="login-form" action="registerProduct" method="post">
        	<label for="nome">Nome do produto</label>
            <input type="text" id="nome" name="nome" required maxlength="100">
            
            <label for="descricao">Descrição</label>
            <input type="text" id="descricao" name="descricao" required maxlength="400">

            <label for="preco">Preço</label>
            <input type="number" id="preco" name="preco" required min="0.05" max="999.99">

			<%-- Provavelmente não será incluído no registro na versão final. --%>
            <label for="estoque">Estoque</label>
            <input type="number" id="estoque" name="estoque" required min="1" max="99">

			<p>Categoria:</p>
				<input type="radio" id="salgado" name="categoria" value="SALGADO">
	  			<label for="salgado">Salgado</label><br>
	  			<input type="radio" id="doce" name="categoria" value="DOCE">
	  			<label for="doce">Doce</label><br>
	  			<input type="radio" id="lanche" name="categoria" value="LANCHE">
	  			<label for="lanche">Lanche</label>
	  			<input type="radio" id="bebida" name="categoria" value="BEBIDA">
	  			<label for="bebida">Bebida</label>
  			
            <button type="submit">Registro</button>
            <a href="home.jsp"><button type="button">Voltar pra home</button></a>
        </form>
    </section>
</body>
</html>
