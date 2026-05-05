import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot_7_idea3 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        String username = null;

        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1);
        }

        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username missing in URL");
            return;
        }

        // Sanitize user input to prevent XSS
        String safeUsername = sanitize(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("");
        resp.getWriter().format("Hello, %s!", safeUsername);
        resp.getWriter().write("");
    }

    /**
     * Basic XSS prevention: escape HTML special characters.
     */
    private String sanitize(String input) {
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