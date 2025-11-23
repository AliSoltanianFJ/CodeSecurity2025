import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot5_idea4 extends HttpServlet {
    private static final Pattern USERNAME_PATH = Pattern.compile("^/([A-Za-z0-9_-]{1,64})$");
    private static final String HTML_TEMPLATE_START =
        "<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body><h1>";
    private static final String HTML_TEMPLATE_END = "</h1></body></html>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String path = req.getPathInfo();
            if (path == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
                return;
            }

            Matcher m = USERNAME_PATH.matcher(path);
            if (!m.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
                return;
            }

            String username = m.group(1);
            String safe = htmlEscape(username);

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html; charset=UTF-8");
            resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self'");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(HTML_TEMPLATE_START + safe + HTML_TEMPLATE_END);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private static String htmlEscape(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '\"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}