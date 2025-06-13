package servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrinho;
import model.ItemCarrinho;

@WebServlet("/carrinho")
public class CarrinhoServlet extends HttpServlet {
   
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");

	    HttpSession session = request.getSession(false);
	    Carrinho carrinho = (session != null) ? (Carrinho) session.getAttribute("carrinho") : null;

	    List<ItemCarrinho> itens = (carrinho != null) ? carrinho.getItens() : new ArrayList<>();

	    List<Map<String, Object>> jsonList = new ArrayList<>();

	    for (ItemCarrinho item : itens) {
	        Map<String, Object> itemJson = new HashMap<>();
	        itemJson.put("id", item.getProduto().getId());
	        itemJson.put("nome", item.getProduto().getNome());
	        itemJson.put("descricao", item.getProduto().getDescricao());
	        itemJson.put("imagemUrl", item.getProduto().getImagem());
	        itemJson.put("preco", item.getProduto().getPreco());
	        itemJson.put("quantidade", item.getQuantidade());
	        itemJson.put("subtotal", item.getSubtotal());
	        jsonList.add(itemJson);
	    }

	    Gson gson = new Gson();
	    String json = gson.toJson(jsonList);
	    response.getWriter().write(json);
	}

}

