<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
    <link rel="stylesheet" href="css/login.css">
</head>
<body>
    <section class="login-page">
        <h1>Login</h1>
        <form class="login-form" action="login" method="post">
            <label for="prontuario">Prontuário</label>
            <input type="text" id="prontuario" name="prontuario" required maxlength="10">

            <label for="senha">Senha</label>
            <input type="password" id="senha" name="senha" required maxlength="100">

            <button type="submit">Login</button>
            <a href="cadastro.jsp"><button type="button">Cadastrar-se</button></a>
        </form>
    </section>
</body>
</html>
