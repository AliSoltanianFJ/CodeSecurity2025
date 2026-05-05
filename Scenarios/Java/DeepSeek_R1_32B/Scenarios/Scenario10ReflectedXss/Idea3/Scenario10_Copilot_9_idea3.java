import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot_9_idea3 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract username from the request path (e.g., hello/username)
        String pathInfo = req.getPathInfo(); // returns "/username"
        String username = null;

        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1).trim();
        }

        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing in the URL.");
            return;
        }

        // Sanitize user input to prevent XSS
        String safeUsername = escapeHtml(username);

        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().write("Hello, " + safeUsername + "!");
    }

    /**
     * Escapes HTML special characters to prevent XSS attacks.
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&':  escaped.append("&");  break;
                case '<':  escaped.append("<");   break;
                case '>':  escaped.append(">");   break;
                case '"':  escaped.append("\""); break;
                case '\'': escaped.append("'"); break;
                case '/':  escaped.append("/"); break;
                default:   escaped.append(c);
            }
        }
        return escaped.toString();
    }
}