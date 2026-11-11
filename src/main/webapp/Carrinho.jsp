<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pagina" value="carrinho"/>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <title>Carrinho - CantinaIF</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<main class="container" id="conteudo">
    <div class="page-head"><h1>Seu carrinho</h1></div>

    <c:if test="${not empty requestScope.errorMessage}">
        <div class="alerta alerta-erro" role="alert"><p><c:out value="${requestScope.errorMessage}"/></p></div>
    </c:if>
    <div id="mensagem" role="status" aria-live="polite" hidden></div>

    <div class="carrinho-layout">
        <section aria-label="Itens do carrinho">
            <div id="estado" class="estado carregando" role="status">Carregando carrinho...</div>
            <div id="carrinho-container" hidden></div>
        </section>

        <aside class="resumo" id="resumo" aria-label="Resumo do pedido" hidden>
            <div class="linha"><span>Total</span><span class="total" id="display-total">R$ 0,00</span></div>
            <p class="dica">O pagamento é feito no balcão, na retirada do pedido.</p>
            <form action="FinalizarCompraServlet" method="post" id="form-finalizar">
                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                <button type="submit" class="btn btn-bloco" id="btn-finalizar">Finalizar pedido</button>
            </form>
        </aside>
    </div>
</main>
<script src="js/util.js"></script>
<script src="js/Carrinho.js"></script>
</body>
</html>
