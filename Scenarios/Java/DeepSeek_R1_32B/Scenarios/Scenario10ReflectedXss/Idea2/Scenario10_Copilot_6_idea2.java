import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot_6_idea2 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");

        // Validate input presence
        if (username == null || username.isEmpty()) {
            sendErrorResponse(resp, 400, "Username is required.");
            return;
        }

        // Sanitize username: only allow letters, digits, and underscores
        if (!isValidUsername(username)) {
            sendErrorResponse(resp, 400, "Invalid username. Only letters, digits, and underscores are allowed.");
            return;
        }

        // Escape HTML to prevent XSS attacks
        String escapedUsername = escapeHtml(username);

        // Prepare and send the response
        resp.setContentType("text/html");
        resp.getWriter().write("<h1>Hello, " + escapedUsername + "!</h1>");
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private void sendErrorResponse(HttpServletResponse resp, int statusCode, String message)
            throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain");
        resp.getWriter().write(message);
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch(c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&apos;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }
}

