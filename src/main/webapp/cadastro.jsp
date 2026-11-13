<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="vendedor" value="${requestScope.perfil == 'vendedor' or (empty requestScope.perfil and param.perfil == 'vendedor')}"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Cadastro - CantinaIF</title>
</head>
<body>
<main class="auth" id="conteudo">
    <section class="auth-card" aria-labelledby="titulo">
        <div class="brand-grande"><img src="img/marca-if.png" alt="" width="33" height="44"><span>CantinaIF</span></div>
        <h1 id="titulo">Criar conta</h1>

        <c:if test="${not empty requestScope.error}">
            <div class="alerta alerta-erro" role="alert"><p><c:out value="${requestScope.error}"/></p></div>
        </c:if>

        <form action="register" method="post" novalidate data-validar id="form-cadastro">
            <input type="hidden" name="_csrf" value="<c:out value='${requestScope.csrfForm}'/>">

            <fieldset class="segmento">
                <legend>Tipo de conta</legend>
                <label><input type="radio" name="perfil" value="aluno" <c:if test="${not vendedor}">checked</c:if>> Sou aluno</label>
                <label><input type="radio" name="perfil" value="vendedor" <c:if test="${vendedor}">checked</c:if>> Sou vendedor</label>
            </fieldset>

            <div class="field">
                <label for="prontuario">Prontuário</label>
                <input type="text" id="prontuario" name="prontuario" required maxlength="10" pattern="[A-Za-z0-9]{1,10}"
                       data-msg-pattern="Use apenas letras e números (até 10 caracteres)." autocomplete="username"
                       value="<c:out value='${requestScope.prontuario}'/>"
                       <c:if test="${not empty requestScope.erros.prontuario}">aria-invalid="true" aria-describedby="erro-prontuario"</c:if>>
                <span class="campo-erro" id="erro-prontuario"><c:out value="${requestScope.erros.prontuario}"/></span>
            </div>

            <fieldset class="perfil-campos" data-perfil="aluno" <c:if test="${vendedor}">hidden disabled</c:if>>
                <div class="field">
                    <label for="nome">Seu nome</label>
                    <input type="text" id="nome" name="nome" required maxlength="75" autocomplete="name"
                           value="<c:out value='${requestScope.nome}'/>"
                           <c:if test="${not empty requestScope.erros.nome}">aria-invalid="true" aria-describedby="erro-nome"</c:if>>
                    <span class="campo-erro" id="erro-nome"><c:out value="${requestScope.erros.nome}"/></span>
                </div>
            </fieldset>

            <fieldset class="perfil-campos" data-perfil="vendedor" <c:if test="${not vendedor}">hidden disabled</c:if>>
                <div class="field">
                    <label for="loja">Nome da cantina</label>
                    <input type="text" id="loja" name="loja" required minlength="2" maxlength="75" autocomplete="organization"
                           value="<c:out value='${requestScope.loja}'/>"
                           <c:if test="${not empty requestScope.erros.loja}">aria-invalid="true" aria-describedby="erro-loja"</c:if>>
                    <span class="dica">É o nome que os seus compradores vão ver.</span>
                    <span class="campo-erro" id="erro-loja"><c:out value="${requestScope.erros.loja}"/></span>
                </div>
            </fieldset>

            <div class="field">
                <label for="email">E-mail institucional</label>
                <input type="email" id="email" name="email" required maxlength="100" autocomplete="email"
                       value="<c:out value='${requestScope.email}'/>"
                       <c:if test="${not empty requestScope.erros.email}">aria-invalid="true" aria-describedby="erro-email"</c:if>>
                <span class="campo-erro" id="erro-email"><c:out value="${requestScope.erros.email}"/></span>
            </div>

            <div class="field">
                <label for="senha">Senha</label>
                <input type="password" id="senha" name="senha" required minlength="8" maxlength="72" autocomplete="new-password"
                       <c:if test="${not empty requestScope.erros.senha}">aria-invalid="true" aria-describedby="erro-senha"</c:if>>
                <span class="dica">Mínimo de 8 caracteres.</span>
                <span class="campo-erro" id="erro-senha"><c:out value="${requestScope.erros.senha}"/></span>
            </div>

            <fieldset class="perfil-campos" data-perfil="aluno" <c:if test="${vendedor}">hidden disabled</c:if>>
                <div class="field">
                    <label for="codigo">Código do vendedor <span class="opcional">(opcional)</span></label>
                    <input type="text" id="codigo" name="codigo" maxlength="9" autocomplete="off" autocapitalize="characters"
                           class="codigo-input" value="<c:out value='${requestScope.codigo}'/>">
                    <span class="dica">Peça o código à cantina: ele vale por 1 minuto. Sem ele você pode criar a conta
                        e se vincular depois, mas só verá produtos após o vínculo.</span>
                </div>
            </fieldset>

            <button type="submit" class="btn btn-bloco">Cadastrar</button>
        </form>
        <div class="auth-links">
            <a class="btn btn-secundario btn-bloco" href="login.jsp">Voltar para o login</a>
        </div>
    </section>
</main>
<script src="js/util.js"></script>
<script src="js/login.js"></script>
</body>
</html>
