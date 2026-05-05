import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Securely map servlet to /hello/* endpoint
@WebServlet("/hello/*")
public class Scenario10_Copilot1_idea4 extends HttpServlet {
    // Override doGet to securely greet the user
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Prevent XSS in older browsers
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        // Prevent clickjacking
        resp.setHeader("X-Frame-Options", "DENY");
        // Prevent MIME sniffing
        resp.setHeader("X-Content-Type-Options", "nosniff");
        // Enforce HTTPS (HSTS)
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Extract username from the path info, e.g., /hello/<username>
        String pathInfo = req.getPathInfo();
        String username = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1); // Remove leading '/'
        }

        // Validate and sanitize username
        // Allow only alphanumeric and limited safe characters, max 32 chars
        if (username == null || !username.matches("^[A-Za-z0-9_\\-]{1,32}$")) {
            // Log the event securely (not shown here), but do not leak details to user
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Context-aware output encoding to prevent XSS
        String safeUsername = htmlEscape(username);

        // Respond with a safe greeting
        resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Minimal HTML escape to prevent XSS.
     * In production, use a well-maintained library like OWASP Java Encoder.
     */
    private String htmlEscape(String input) {
        if (input == null) return "";
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
