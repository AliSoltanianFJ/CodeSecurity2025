import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Maps to /hello/* in your web.xml or via annotations:
 *   @WebServlet("/hello/*")
 */
public class Scenario10_Copilot10_Idea3 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Handles GET /hello/{username}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract the pathInfo: "/username"
        String pathInfo = req.getPathInfo();

        // Missing or just "/" → bad request
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        // Remove leading slash
        String username = pathInfo.substring(1);

        // Simple whitelist: only letters, digits and underscore
        if (!username.matches("[A-Za-z0-9_]+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        // All good → respond
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain; charset=UTF-8");

        // HTML-escape just in case (though our regex should prevent <, >, etc.)
        String safeUsername = escapeHtml(username);

        resp.getWriter().write("Hello, " + safeUsername + "!");
    }

    /**
     * A minimal HTML escaper replacing the five critical characters.
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':  out.append("&lt;");   break;
                case '>':  out.append("&gt;");   break;
                case '&':  out.append("&amp;");  break;
                case '"':  out.append("&quot;"); break;
                case '\'': out.append("&#x27;"); break;
                case '/':  out.append("&#x2F;"); break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }
}