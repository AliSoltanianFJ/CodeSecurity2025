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

public class Scenario10_Copilot3_idea4 extends HttpServlet {
    // Username validation pattern: only allows alphanumeric, underscores, and hyphens (min 3, max 30 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");

    /**
     * Securely says hello to the user.
     * - Validates and sanitizes username input.
     * - Uses context-aware output encoding to prevent XSS.
     * - Handles errors without leaking sensitive information.
     * - Assumes HTTPS is enforced at the server/container level.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setHeader("Content-Type", "text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        String username = req.getParameter("username");
        String safeUsername;

        try {
            // Validate username input
            if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
                // Log the event securely (do not log user input directly)
                // Respond with a generic error message
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("<html><body><h2>Invalid username.</h2></body></html>");
                return;
            }

            // Context-aware HTML encoding to prevent XSS
            safeUsername = Encode.forHtml(username);

            // Principle of least privilege: only greet, no sensitive operations
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("<html><body><h2>Hello, " + safeUsername + "!</h2></body></html>");
        } catch (Exception ex) {
            // Proper error handling: do not leak internal details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("<html><body><h2>An unexpected error occurred.</h2></body></html>");
            // Optionally log the exception securely (not shown here)
        }
    }
}
