<%@ tag pageEncoding="UTF-8" body-content="scriptless" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%-- Notificacao (popup) vinda do servidor; o mesmo markup que notificar() cria em js/util.js.
     tipo: ok | erro | aviso. "ok" some sozinho; os outros ficam ate o X. chave: fechada, nao volta nesta sessao. --%>
<%@ attribute name="tipo" required="true" %>
<%@ attribute name="chave" required="false" %>
<div class="notificacao notificacao-${tipo}" role="${tipo == 'erro' ? 'alert' : 'status'}"<c:if test="${tipo == 'ok'}"> data-some</c:if><c:if test="${not empty chave}"> data-chave="<c:out value='${chave}'/>"</c:if>>
    <p class="notificacao-texto"><jsp:doBody/></p>
    <button type="button" class="notificacao-fechar js-fechar-notificacao" aria-label="Fechar notificação" title="Fechar"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M6 6l12 12M18 6L6 18"/></svg></button>
</div>
