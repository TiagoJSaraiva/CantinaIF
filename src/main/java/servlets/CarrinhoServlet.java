package servlets;

import java.io.BufferedReader;
import java.io.IOException;
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

@WebServlet("/carrinho")
public class CarrinhoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Adiciona um produto ao carrinho da sessao. Corpo JSON: {produtoId, quantidade}. */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		int produtoId;
		int quantidade;
		try (BufferedReader reader = request.getReader()) {
			JsonObject json = new Gson().fromJson(reader, JsonObject.class);
			produtoId = lerInt(json, "produtoId");
			quantidade = lerInt(json, "quantidade");
		} catch (RuntimeException e) {
			Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Requisição inválida.");
			return;
		}
		if (quantidade < 1) {
			Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "A quantidade deve ser de pelo menos 1.");
			return;
		}

		Produto produto = new ProductDao().buscarPorId(produtoId);
		if (produto == null) {
			Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Produto não encontrado.");
			return;
		}

		HttpSession session = request.getSession();
		Carrinho carrinho = (Carrinho) session.getAttribute("carrinho");
		if (carrinho == null) {
			carrinho = new Carrinho();
			session.setAttribute("carrinho", carrinho);
		}

		// Considera o que ja esta no carrinho ao checar o estoque
		int jaNoCarrinho = 0;
		for (ItemCarrinho item : carrinho.getItens()) {
			if (item.getProduto().getId() == produtoId) {
				jaNoCarrinho = item.getQuantidade();
			}
		}
		if (jaNoCarrinho + quantidade > produto.getEstoque()) {
			Json.result(response, HttpServletResponse.SC_CONFLICT, false, "Estoque insuficiente para a quantidade pedida.");
			return;
		}

		carrinho.adicionarProduto(produto, quantidade);
		Json.result(response, HttpServletResponse.SC_OK, true, "Produto adicionado ao carrinho.");
	}

	private static int lerInt(JsonObject json, String campo) {
		JsonElement e = (json == null) ? null : json.get(campo);
		if (e == null || e.isJsonNull()) {
			throw new IllegalArgumentException("Campo ausente: " + campo);
		}
		return e.getAsInt();
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
			itemJson.put("temImagem", p.getImagem() != null && p.getImagem().length > 0);
			jsonList.add(itemJson);
		}
		Json.write(response, HttpServletResponse.SC_OK, jsonList);
	}
}
