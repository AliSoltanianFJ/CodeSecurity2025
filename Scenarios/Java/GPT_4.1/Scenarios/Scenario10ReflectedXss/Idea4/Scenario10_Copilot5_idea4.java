import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// Security decisions:
// - Input is strictly validated and sanitized to prevent XSS and injection attacks.
// - Output is contextually encoded for HTML.
// - No sensitive information is leaked in error messages.
// - No secrets or credentials are hardcoded.
// - Principle of least privilege: no unnecessary permissions or operations.
// - All exceptions are handled gracefully.

public class Scenario10_Copilot5_idea4 extends HttpServlet {
    // Allowed username pattern: alphanumeric, underscores, hyphens, 3-32 chars
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_-]{3,32}$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce secure response headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';");

        // Ensure response uses UTF-8
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/html; charset=UTF-8");

        String username = req.getParameter("username");
        String safeUsername;

        if (username == null) {
            // Do not leak internal details
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("<html><body><h2>Missing username parameter.</h2></body></html>");
            return;
        }

        // Validate username strictly
        if (!username.matches(USERNAME_PATTERN)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("<html><body><h2>Invalid username format.</h2></body></html>");
            return;
        }

        // Context-aware HTML encoding to prevent XSS
        safeUsername = htmlEncode(username);

        // Respond securely
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("<html><body><h2>Hello, " + safeUsername + "!</h2></body></html>");
    }

    /**
     * Encodes input for safe HTML display.
     * Prevents XSS by escaping special characters.
     */
    private String htmlEncode(String input) {
        StringBuilder encoded = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': encoded.append("&amp;"); break;
                case '<': encoded.append("&lt;"); break;
                case '>': encoded.append("&gt;"); break;
                case '"': encoded.append("&quot;"); break;
                case '\'': encoded.append("&#x27;"); break;
                case '/': encoded.append("&#x2F;"); break;
                default: encoded.append(c);
            }
        }
        return encoded.toString();
    }
}
