<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="cif" %>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Entrar - CantinaIF</title>
</head>
<body>
<main class="auth" id="conteudo">
    <section class="auth-card" aria-labelledby="titulo">
        <%@ include file="/WEB-INF/jspf/botao-tema.jspf" %>
        <div class="brand-grande"><img src="img/marca-if.png" alt="" width="33" height="44"><span>CantinaIF</span></div>
        <h1 id="titulo">Entrar</h1>

        <div class="notificacoes" id="notificacoes" aria-live="polite">
            <c:if test="${param.restaurada == '1'}">
                <cif:notificacao tipo="aviso">A demonstração foi restaurada; entre novamente.</cif:notificacao>
            </c:if>
            <c:if test="${param.cadastro == 'ok' and empty param.vinculo}">
                <cif:notificacao tipo="ok">Cadastro realizado. Faça login para continuar.</cif:notificacao>
            </c:if>
            <c:if test="${param.cadastro == 'ok' and param.vinculo == 'ok'}">
                <cif:notificacao tipo="ok">Cadastro realizado e conta vinculada à cantina. Faça login para ver os produtos.</cif:notificacao>
            </c:if>
            <c:if test="${param.cadastro == 'ok' and param.vinculo == 'falhou'}">
                <cif:notificacao tipo="aviso">Conta criada, mas o código do vendedor era inválido ou expirou.
                    Entre e peça um novo código para se vincular.</cif:notificacao>
            </c:if>
            <c:if test="${param.cadastro == 'vendedor'}">
                <cif:notificacao tipo="ok">Conta de vendedor criada. Entre para cadastrar seus produtos
                    e gerar códigos de vínculo para os seus compradores.</cif:notificacao>
            </c:if>
            <c:if test="${not empty requestScope.error}">
                <cif:notificacao tipo="erro"><c:out value="${requestScope.error}"/></cif:notificacao>
            </c:if>
        </div>

        <c:if test="${applicationScope.demoMode}">
            <section class="demo-box" aria-labelledby="demo-titulo">
                <h2 id="demo-titulo">Contas de demonstração</h2>
                <p>Dados fictícios, restaurados automaticamente
                    <c:choose>
                        <c:when test="${applicationScope.demoResetHoras > 0}">a cada <c:out value="${applicationScope.demoResetTexto}"/> h.</c:when>
                        <c:otherwise>a cada reinício.</c:otherwise>
                    </c:choose>
                </p>
                <ul>
                    <li><span>Aluno: <code><c:out value="${applicationScope.demoComprador}"/></code></span>
                        <button type="button" class="btn btn-secundario btn-pequeno js-demo"
                                data-prontuario="<c:out value='${applicationScope.demoComprador}'/>"
                                data-senha="<c:out value='${applicationScope.demoSenha}'/>">Usar esta conta</button></li>
                    <li><span>Vendedor (Cantina Central): <code><c:out value="${applicationScope.demoVendedor}"/></code></span>
                        <button type="button" class="btn btn-secundario btn-pequeno js-demo"
                                data-prontuario="<c:out value='${applicationScope.demoVendedor}'/>"
                                data-senha="<c:out value='${applicationScope.demoSenha}'/>">Usar esta conta</button></li>
                    <li><span>Vendedor (Lanchonete do Bloco B): <code><c:out value="${applicationScope.demoVendedor2}"/></code></span>
                        <button type="button" class="btn btn-secundario btn-pequeno js-demo"
                                data-prontuario="<c:out value='${applicationScope.demoVendedor2}'/>"
                                data-senha="<c:out value='${applicationScope.demoSenha}'/>">Usar esta conta</button></li>
                </ul>
                <p class="dica">Senha das três contas: <code><c:out value="${applicationScope.demoSenha}"/></code>.
                    O aluno está vinculado à Cantina Central; para testar outro vínculo, crie uma conta e use um código
                    gerado em Compradores.</p>
            </section>
        </c:if>

        <form action="login" method="post" novalidate data-validar>
            <input type="hidden" name="_csrf" value="<c:out value='${requestScope.csrfForm}'/>">
            <div class="field">
                <label for="prontuario">Prontuário</label>
                <input type="text" id="prontuario" name="prontuario" required maxlength="10" autocomplete="username" autocapitalize="characters">
                <span class="campo-erro" id="erro-prontuario"></span>
            </div>
            <div class="field">
                <label for="senha">Senha</label>
                <input type="password" id="senha" name="senha" required maxlength="100" autocomplete="current-password">
                <span class="campo-erro" id="erro-senha"></span>
            </div>
            <button type="submit" class="btn btn-bloco">Entrar</button>
        </form>
        <div class="auth-links">
            <a class="btn btn-secundario btn-bloco" href="cadastro.jsp">Criar conta de aluno</a>
            <a class="btn btn-secundario btn-bloco" href="cadastro.jsp?perfil=vendedor">Criar conta de vendedor</a>
        </div>
    </section>
</main>
<script src="js/util.js"></script>
<script src="js/login.js"></script>
</body>
</html>
