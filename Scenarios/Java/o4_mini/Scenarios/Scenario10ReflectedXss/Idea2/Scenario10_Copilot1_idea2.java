import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "Scenario10", urlPatterns = {"/hello/*"})
public class Scenario10_Copilot1_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract the path info (e.g. "/username")
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        // Remove leading slash
        String rawUsername = pathInfo.substring(1);

        // Validate input: only letters, digits, underscore, hyphen; length 1–32
        if (!rawUsername.matches("^[A-Za-z0-9_\\-]{1,32}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        // Sanitize to prevent XSS
        String username = htmlEscape(rawUsername);

        // Set safe headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // Write response
        resp.getWriter().printf(
            "<!DOCTYPE html>"
          + "<html><head><meta charset=\"UTF-8\"><title>Hello</title></head>"
          + "<body><h1>Hello, %s!</h1></body></html>",
            username
        );
    }

    /**
     * Basic HTML escaping to prevent XSS.
     */
    private String htmlEscape(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&':  escaped.append("&amp;");  break;
                case '<':  escaped.append("&lt;");   break;
                case '>':  escaped.append("&gt;");   break;
                case '"':  escaped.append("&quot;"); break;
                case '\'': escaped.append("&#x27;"); break;
                case '/':  escaped.append("&#x2F;"); break;
                default:   escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
