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

public class Scenario10_Copilot1_idea4 extends HttpServlet {
    // Username validation pattern: only allows alphanumeric and underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    /**
     * Securely says hello to the user.
     * - Validates and sanitizes the username parameter.
     * - Uses context-aware output encoding to prevent XSS.
     * - Handles errors without leaking sensitive information.
     * - Assumes HTTPS is enforced at the server configuration level.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setHeader("Content-Type", "text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains"); // Enforce HTTPS

        String username = req.getParameter("username");
        String safeUsername;

        try {
            // Validate username input
            if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
                // Log the event securely (do not log user input directly)
                // Respond with generic error message
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("<html><body>Invalid username.</body></html>");
                return;
            }

            // Context-aware output encoding to prevent XSS
            safeUsername = Encode.forHtml(username);

            // Respond securely
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("<html><body>Hello, " + safeUsername + "!</body></html>");
        } catch (Exception ex) {
            // Handle unexpected errors securely
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("<html><body>An unexpected error occurred.</body></html>");
            // Optionally log exception details to a secure log (not shown here)
        }
    }
}
