<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cadastro</title>
    <link rel="stylesheet" href="css/login.css">
</head>
<body>
    <section class="login-page">
        <h1>Cadastro</h1>
        <form class="login-form" action="register" method="post">
            <label for="prontuario">Prontuário</label>
            <input type="text" id="prontuario" name="prontuario" required>
            
            <label for="nome">Seu nome</label>
            <input type="text" id="nome" name="nome" required>

            <label for="email">E-mail institucional</label>
            <input type="email" id="email" name="email" required>

            <label for="senha">Senha</label>
            <input type="password" id="senha" name="senha" required>

            <button type="submit">Cadastro</button>
            <a href="login.jsp"><button type="button">Voltar pro login</button></a>
        </form>
    </section>
</body>
</html>
