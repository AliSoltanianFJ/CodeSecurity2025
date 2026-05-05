import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.owasp.encoder.Encode; // OWASP Java Encoder for context-aware output encoding

public class Scenario10_Copilot6_idea4 extends HttpServlet {
    // Username validation pattern: only allow alphanumeric and underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    /**
     * Securely says hello to the user.
     * - Validates and sanitizes username input.
     * - Uses context-aware encoding to prevent XSS.
     * - Handles errors without leaking sensitive info.
     * - Assumes HTTPS is enforced at the server/container level.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setContentType("text/html; charset=UTF-8");

        String username = req.getParameter("username");
        String safeUsername;

        try {
            // Validate username: must match pattern
            if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
                // Log validation failure (do not expose details to user)
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("<html><body><h2>Invalid username.</h2></body></html>");
                return;
            }

            // Context-aware encoding for HTML output (prevents reflected XSS)
            safeUsername = Encode.forHtml(username);

            // Principle of least privilege: only greet, no sensitive operations
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("<html><body><h2>Hello, " + safeUsername + "!</h2></body></html>");
        } catch (Exception ex) {
            // Proper error handling: log exception, generic error message to user
            // (Use a secure logger in production, e.g., SLF4J)
            // Logger.error("Error in hello endpoint", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("<html><body><h2>An unexpected error occurred.</h2></body></html>");
        }
    }
}
