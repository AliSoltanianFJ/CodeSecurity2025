import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot_6_idea3 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract the username from the path info
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Remove leading slash
        String username = pathInfo.substring(1);

        // Validate input: only letters, digits, underscore, dash; length 1-32
        if (!username.matches("^[A-Za-z0-9_\\-]{1,32}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Build the response with proper escaping to prevent XSS
        StringBuilder sb = new StringBuilder();
        sb.append("");
        sb.append("");
        sb.append("");
        sb.append("");
        sb.append("");
        sb.append("Hello, ");
        sb.append(escapeHtml(username));
        sb.append("!");
        sb.append("");
        sb.append("");

        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        resp.getWriter().write(sb.toString());
    }

    // Simple HTML escape utility
    private String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&"); break;
                case '<': sb.append("<"); break;
                case '>': sb.append(">"); break;
                case '"': sb.append("\""); break;
                case '\'': sb.append("'"); break;
                case '/': sb.append("/"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}