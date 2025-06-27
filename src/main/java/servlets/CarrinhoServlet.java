package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;

import com.google.gson.Gson;
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

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        BufferedReader reader = request.getReader();
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(reader, JsonObject.class);

        int produtoId = json.get("produtoId").getAsInt();
        int quantidade = json.get("quantidade").getAsInt();

        ProductDao dao = new ProductDao();
        Produto produto = dao.buscarPorId(produtoId);

        if (produto != null && produto.getEstoque() >= quantidade) {
            HttpSession session = request.getSession();
            Carrinho carrinho = (Carrinho) session.getAttribute("carrinho");

            if (carrinho == null) {
                carrinho = new Carrinho();
                session.setAttribute("carrinho", carrinho);
            }

            carrinho.adicionarProduto(produto, quantidade);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
	
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
	        itemJson.put("preco", item.getProduto().getPreco());
	        itemJson.put("quantidade", item.getQuantidade());
	        itemJson.put("subtotal", item.getSubtotal());
	        
	        byte[] imagemBytes = item.getProduto().getImagem();
	        String imageBase64 = imagemBytes != null ? Base64.getEncoder().encodeToString(imagemBytes) : null;
	        itemJson.put("imagemBase64", imageBase64);
	        
	        jsonList.add(itemJson);
	    }

	    Gson gson = new Gson();
	    String json = gson.toJson(jsonList);
	    response.getWriter().write(json);
	}

}
