package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dao.ProductDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrinho;
import model.ItemCarrinho;
import model.Produto;

/**
 * Carrinho da sessao. GET lista; POST adiciona {produtoId, quantidade}; PUT define a quantidade
 * absoluta de um item {produtoId, quantidade}; DELETE remove (?produtoId=).
 */
@WebServlet("/carrinho")
public class CarrinhoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final ProductDao productDao = new ProductDao();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		alterar(request, response, true);
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		alterar(request, response, false);
	}

	private void alterar(HttpServletRequest request, HttpServletResponse response, boolean somar) throws IOException {
		int produtoId;
		int quantidade;
		try (BufferedReader reader = request.getReader()) {
			JsonObject json = new Gson().fromJson(reader, JsonObject.class);
			produtoId = lerInt(json, "produtoId");
			quantidade = lerInt(json, "quantidade");
		} catch (RuntimeException e) {
			for (Throwable t = e; t != null; t = t.getCause()) {
				if (t instanceof BodyLimitFilter.CorpoGrandeDemaisException) {
					Json.result(response, 413, false, "Requisição grande demais.");
					return;
				}
			}
			Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Requisição inválida.");
			return;
		}
		if (quantidade < 1 || quantidade > Carrinho.MAX_POR_ITEM) {
			Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false,
					"A quantidade deve ser de 1 a " + Carrinho.MAX_POR_ITEM + ".");
			return;
		}

		Produto produto;
		model.EstadoComprador estado;
		try {
			estado = Sessao.estado(request);
			produto = productDao.buscarResumo(produtoId);
		} catch (SQLException e) {
			e.printStackTrace();
			Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao consultar o produto.");
			return;
		}
		if (estado == null || !estado.vinculado()) {
			Json.result(response, HttpServletResponse.SC_CONFLICT, false, "Vincule sua conta a um vendedor para comprar.");
			return;
		}
		// so produtos do vendedor ao qual o comprador esta vinculado (os de outro vendedor "nao existem" para ele)
		if (produto == null || !estado.vendedorProntuario().equals(produto.getVendedorProntuario())) {
			Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Produto não encontrado.");
			return;
		}
		if (produto.getEstoque() < 1) {
			Json.result(response, HttpServletResponse.SC_CONFLICT, false, "Produto esgotado.");
			return;
		}

		Carrinho carrinho = obterOuCriarCarrinho(request.getSession());
		// a conferencia de estoque e a alteracao do carrinho acontecem juntas (sem corrida entre requisicoes)
		synchronized (carrinho) {
			long jaNoCarrinho = carrinho.quantidadeDe(produtoId);
			long desejada = somar ? jaNoCarrinho + quantidade : quantidade; // long: sem estouro de int
			if (desejada > produto.getEstoque()) {
				Json.result(response, HttpServletResponse.SC_CONFLICT, false,
						"Estoque insuficiente: restam " + produto.getEstoque() + " unidade(s) de " + produto.getNome() + ".");
				return;
			}
			if (desejada > Carrinho.MAX_POR_ITEM) {
				Json.result(response, HttpServletResponse.SC_CONFLICT, false,
						"O máximo é " + Carrinho.MAX_POR_ITEM + " unidades por item.");
				return;
			}
			if (somar) {
				carrinho.adicionarProduto(produto, quantidade);
			} else if (!carrinho.alterarQuantidade(produtoId, quantidade)) {
				Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Item não está no carrinho.");
				return;
			}
		}
		Json.result(response, HttpServletResponse.SC_OK, true, somar ? "Produto adicionado ao carrinho." : "Quantidade atualizada.");
	}

	private static final Object CRIACAO = new Object();

	/** Cria o carrinho da sessao uma unica vez, mesmo com varias requisicoes simultaneas numa sessao nova. */
	static Carrinho obterOuCriarCarrinho(HttpSession session) {
		synchronized (CRIACAO) {
			Carrinho carrinho = (Carrinho) session.getAttribute("carrinho");
			if (carrinho == null) {
				carrinho = new Carrinho();
				session.setAttribute("carrinho", carrinho);
			}
			return carrinho;
		}
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			int produtoId = Integer.parseInt(request.getParameter("produtoId"));
			HttpSession session = request.getSession(false);
			Carrinho carrinho = (session != null) ? (Carrinho) session.getAttribute("carrinho") : null;
			if (carrinho != null) {
				carrinho.removerProduto(produtoId);
			}
			Json.result(response, HttpServletResponse.SC_OK, true, "Item removido.");
		} catch (NumberFormatException e) {
			Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Requisição inválida.");
		}
	}

	private static int lerInt(JsonObject json, String campo) {
		JsonElement e = (json == null) ? null : json.get(campo);
		return inteiroEstrito(e, campo);
	}

	/**
	 * Converte um valor JSON em int SEM truncar: "1.5" ou 1.5 sao rejeitados (400) em vez de virar 1; tambem
	 * rejeita objetos, listas, nulos, texto nao numerico e valores fora do intervalo de int.
	 */
	static int inteiroEstrito(JsonElement e, String campo) {
		if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) {
			throw new IllegalArgumentException("Campo ausente ou invalido: " + campo);
		}
		try {
			java.math.BigDecimal valor = new java.math.BigDecimal(e.getAsString().trim());
			return valor.intValueExact(); // lanca ArithmeticException se tiver parte fracionaria ou estourar int
		} catch (NumberFormatException | ArithmeticException ex) {
			throw new IllegalArgumentException("Valor inteiro invalido em " + campo, ex);
		}
	}

	/** Lista os itens do carrinho da sessao como JSON plano. */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		Carrinho carrinho = (session != null) ? (Carrinho) session.getAttribute("carrinho") : null;
		List<ItemCarrinho> itens = (carrinho != null) ? carrinho.getItens() : new ArrayList<>();

		List<Map<String, Object>> jsonList = new ArrayList<>();
		for (ItemCarrinho item : itens) {
			Produto p = item.getProduto();
			Map<String, Object> itemJson = new LinkedHashMap<>();
			itemJson.put("id", p.getId());
			itemJson.put("nome", p.getNome());
			itemJson.put("descricao", p.getDescricao());
			itemJson.put("preco", p.getPreco());
			itemJson.put("quantidade", item.getQuantidade());
			itemJson.put("subtotal", item.getSubtotal());
			itemJson.put("estoque", p.getEstoque());
			itemJson.put("temImagem", p.isTemImagem());
			jsonList.add(itemJson);
		}
		response.setHeader("Cache-Control", "no-store");
		Json.write(response, HttpServletResponse.SC_OK, jsonList);
	}
}
