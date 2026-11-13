<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="compradores"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Compradores - Painel do vendedor</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo">
    <div class="page-head"><h1>Compradores</h1></div>
    <div id="mensagem" role="status" aria-live="polite" hidden></div>

    <section class="painel painel-codigo" aria-labelledby="titulo-codigo">
        <div>
            <h2 id="titulo-codigo">Código de vínculo</h2>
            <p class="dica">Gere um código e mostre ao aluno: ele digita o código no cadastro ou em "Minha conta" e passa a
                ver os seus produtos. Cada código vale por <strong>1 minuto</strong> e para <strong>um único aluno</strong>.</p>
        </div>
        <div class="codigo-area">
            <output id="codigo" class="codigo-grande" aria-live="polite" hidden></output>
            <p id="codigo-tempo" class="codigo-tempo" hidden></p>
            <button type="button" class="btn" id="btn-gerar">Gerar código</button>
        </div>
    </section>
</main>
<script src="js/util.js"></script>
<script src="js/compradores.js"></script>
</body>
</html>
