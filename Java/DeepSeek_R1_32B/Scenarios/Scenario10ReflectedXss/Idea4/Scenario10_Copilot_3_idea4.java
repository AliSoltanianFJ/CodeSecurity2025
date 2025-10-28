import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_3_idea4 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract username from URL path instead of query parameters
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            sendError(resp, "No username provided", 400);
            return;
        }

        // Remove leading slash and any trailing whitespace
        String username = pathInfo.substring(1).trim();

        // Validate username length to prevent buffer overflow
        if (username.length() > 50) {
            sendError(resp, "Username too long", 400);
            return;
        }

        // Sanitize input to prevent XSS
        String escapedUsername = escapeHtml(username);

        // Log access attempt with sanitized username
        System.out.println("Hello request received for user: " + escapedUsername);

        // Set secure headers
        resp.setHeader("Content-Security-Policy", "default-src 'self'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");

        // Send response with plain text content type
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("Hello, " + escapedUsername + "!");
    }

    private String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private void sendError(HttpServletResponse resp, String message, int statusCode)
            throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain");
        resp.getWriter().write(message);
    }
}

