<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <section class="login-page">
        <h1>Login</h1>
        <form class="login-form" action="login" method="post">
            <label for="prontuario">Prontuário</label>
            <input type="text" id="prontuario" name="prontuario" required>

            <label for="senha">Senha</label>
            <input type="password" id="senha" name="senha" required>

            <button type="submit">Login</button>
            <a href="cadastro.jsp"><button type="button">Cadastro</button></a>
        </form>
    </section>
</body>
</html>
