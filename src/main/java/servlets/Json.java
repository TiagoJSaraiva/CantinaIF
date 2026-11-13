package servlets;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletResponse;

/** Utilitario para respostas JSON dos servlets. */
final class Json {

    private static final Gson GSON = new Gson();

    private Json() {
    }

    static void write(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(body));
    }

    /** Data/hora local em ISO-8601 com o deslocamento do fuso do servidor (null se nula). */
    static String iso(java.time.LocalDateTime data) {
        return data == null ? null
                : data.atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** Resposta padrao {success, message}. */
    static void result(HttpServletResponse response, int status, boolean success, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        body.put("message", message);
        write(response, status, body);
    }

    /** Erros de validacao por campo: 400 {success:false, message, erros:{campo: mensagem}}. */
    static void erros(HttpServletResponse response, Map<String, String> erros) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Corrija os campos destacados.");
        body.put("erros", erros);
        write(response, HttpServletResponse.SC_BAD_REQUEST, body);
    }
}
