import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Securely map the servlet to /hello/* endpoint
@WebServlet("/hello/*")
public class Scenario10_Copilot2_idea4 extends HttpServlet {
    // Maximum allowed username length to prevent resource exhaustion
    private static final int MAX_USERNAME_LENGTH = 50;

    /**
     * Handles GET requests to /hello/<username>
     * Responds with a personalized hello message.
     * All user input is strictly validated and contextually encoded to prevent XSS and other attacks.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");

        // Extract username from the path info (e.g., /hello/<username>)
        String pathInfo = req.getPathInfo();
        String username = null;

        if (pathInfo != null && pathInfo.length() > 1) {
            // Remove leading slash
            username = pathInfo.substring(1);
        }

        // Validate and sanitize username
        if (username == null || username.isEmpty()) {
            // Do not leak internal details in error messages
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required.");
            return;
        }

        // Enforce maximum length and allow only safe characters (alphanumeric, underscore, hyphen)
        if (username.length() > MAX_USERNAME_LENGTH ||
                !username.matches("^[A-Za-z0-9_-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Contextually encode username for HTML output to prevent XSS
        String safeUsername = htmlEncode(username);

        // Respond with a safe hello message
        resp.getWriter().write("<!DOCTYPE html>\n<html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Minimal HTML encoding to prevent XSS.
     * In production, use a well-maintained library such as OWASP Java Encoder.
     */
    private static String htmlEncode(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
