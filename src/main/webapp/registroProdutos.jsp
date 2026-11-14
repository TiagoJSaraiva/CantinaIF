<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="produtos"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Produtos - Painel do vendedor</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo">
    <div class="page-head">
        <h1>Produtos</h1>
        <button type="button" class="btn" id="btn-novo">+ Novo produto</button>
    </div>

    <div class="toolbar">
        <div class="busca">
            <label for="busca" class="sr-only">Buscar produtos</label>
            <input type="search" id="busca" placeholder="Buscar produto..." autocomplete="off">
        </div>
        <div class="chips" role="group" aria-label="Filtrar por categoria">
            <button type="button" class="chip" data-filter="todos" aria-pressed="true">Todos</button>
            <button type="button" class="chip" data-filter="salgado" aria-pressed="false">Salgados</button>
            <button type="button" class="chip" data-filter="lanche" aria-pressed="false">Lanches</button>
            <button type="button" class="chip" data-filter="doce" aria-pressed="false">Doces</button>
            <button type="button" class="chip" data-filter="bebida" aria-pressed="false">Bebidas</button>
        </div>
    </div>

    <div id="estado" class="estado carregando" role="status">Carregando produtos...</div>
    <div id="productContainer" class="grade" hidden></div>
</main>

<dialog id="dlg-produto" aria-labelledby="dlg-titulo">
    <h2 id="dlg-titulo">Novo produto</h2>
    <form id="form-produto" novalidate data-validar>
        <input type="hidden" name="id" id="produto-id">
        <div class="field">
            <label for="produto-nome">Nome</label>
            <input type="text" id="produto-nome" name="nome" required maxlength="100">
            <span class="campo-erro" id="erro-nome"></span>
        </div>
        <div class="field">
            <label for="produto-descricao">Descrição</label>
            <textarea id="produto-descricao" name="descricao" maxlength="400"></textarea>
            <span class="campo-erro" id="erro-descricao"></span>
        </div>
        <div class="linha-campos">
            <div class="field">
                <label for="produto-preco">Preço (R$)</label>
                <input type="number" id="produto-preco" name="preco" required min="0" max="99999999.99" step="0.01" inputmode="decimal">
                <span class="campo-erro" id="erro-preco"></span>
            </div>
            <div class="field">
                <label for="produto-estoque">Estoque</label>
                <input type="number" id="produto-estoque" name="estoque" required min="0" max="1000000" step="1" inputmode="numeric">
                <span class="campo-erro" id="erro-estoque"></span>
            </div>
        </div>
        <div class="field">
            <label for="produto-categoria">Categoria</label>
            <select id="produto-categoria" name="categoria" required>
                <option value="">Selecione...</option>
                <option value="salgado">Salgado</option>
                <option value="lanche">Lanche</option>
                <option value="doce">Doce</option>
                <option value="bebida">Bebida</option>
            </select>
            <span class="campo-erro" id="erro-categoria"></span>
        </div>
        <div class="field">
            <label for="produto-imagem">Imagem (JPEG, PNG ou GIF, até 2 MB; será reduzida para 640 px)</label>
            <input type="file" id="produto-imagem" name="imagem" accept="image/jpeg,image/png,image/gif">
            <span class="dica" id="dica-imagem">Opcional. Ao editar, deixe em branco para manter a imagem atual.</span>
            <span class="campo-erro" id="erro-imagem"></span>
        </div>
        <div class="alerta alerta-erro" id="erro-geral" role="alert" hidden></div>
        <div class="dialogo-acoes">
            <button type="button" class="btn btn-secundario" id="btn-cancelar-dlg">Cancelar</button>
            <button type="submit" class="btn" id="btn-salvar">Salvar</button>
        </div>
    </form>
</dialog>

<script src="js/util.js"></script>
<script src="js/ADMfiltro.js"></script>
</body>
</html>
