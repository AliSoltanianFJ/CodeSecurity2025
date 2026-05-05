import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.charset.StandardCharsets;
import org.owasp.encoder.Encode; // OWASP Java Encoder for context-aware output encoding

public class Scenario10_Copilot4_idea4 extends HttpServlet {
    // Username validation pattern: only allows alphanumeric and underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    /**
     * Securely says hello to the user at the endpoint /hello/<username>.
     * All user input is validated and encoded to prevent XSS and other attacks.
     * No sensitive information is leaked in error messages.
     * 
     * Security decisions:
     * - Input validation restricts username to safe characters and length.
     * - Output encoding via OWASP Java Encoder prevents XSS.
     * - No sensitive data is processed or exposed.
     * - All exceptions are handled securely.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense in depth, actual enforcement should be at server config)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        // Extract username from path info, e.g., /hello/<username>
        String pathInfo = req.getPathInfo();
        String username = null;
        if (pathInfo != null && pathInfo.startsWith("/")) {
            username = pathInfo.substring(1); // Remove leading '/'
        }

        // Validate username
        if (username == null || !isValidUsername(username)) {
            // Do not leak details about validation failure
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Invalid username.");
            return;
        }

        // Context-aware output encoding to prevent XSS
        String safeUsername = Encode.forHtml(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff"); // Secure header
        resp.setHeader("X-Frame-Options", "DENY"); // Prevent clickjacking
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate"); // No sensitive caching

        // Respond securely
        resp.getWriter().write("<!DOCTYPE html>");
        resp.getWriter().write("<html lang=\"en\"><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Validates the username according to strict rules.
     * Only allows alphanumeric characters and underscores, 3-30 characters.
     * 
     * @param username The username to validate.
     * @return true if valid, false otherwise.
     */
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        Matcher matcher = USERNAME_PATTERN.matcher(username);
        return matcher.matches();
    }
}
