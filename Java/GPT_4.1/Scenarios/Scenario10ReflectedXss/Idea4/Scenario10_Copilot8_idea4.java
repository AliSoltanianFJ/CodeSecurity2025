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

public class Scenario10_Copilot8_idea4 extends HttpServlet {
    // Username validation pattern: only allow alphanumeric, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,30}$");

    /**
     * Securely says hello to the user.
     * - Validates and sanitizes user input.
     * - Uses context-aware output encoding to prevent XSS.
     * - Handles errors without leaking sensitive info.
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
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        String username = req.getParameter("username");
        String safeUsername;

        try {
            // Validate username: only allow alphanumeric, 3-30 chars
            if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
                // Log event securely (not shown here; use a secure logging framework)
                safeUsername = "Guest";
            } else {
                // Context-aware HTML encoding to prevent XSS
                safeUsername = Encode.forHtml(username);
            }

            // Respond with a safe hello message
            resp.getWriter().write("<!DOCTYPE html><html><body>");
            resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
            resp.getWriter().write("</body></html>");
        } catch (Exception e) {
            // Generic error message, no sensitive info leaked
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("<!DOCTYPE html><html><body>");
            resp.getWriter().write("<h1>An unexpected error occurred.</h1>");
            resp.getWriter().write("</body></html>");
            // Optionally log the exception securely (not shown here)
        }
    }
}
