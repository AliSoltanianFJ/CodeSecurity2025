import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Secure Hello Servlet
 * 
 * Security decisions:
 * - Input validation and sanitization for 'username' to prevent XSS and injection attacks.
 * - Output encoding for HTML context.
 * - No sensitive information is leaked in error messages.
 * - No hardcoded secrets or credentials.
 * - Principle of least privilege: No privileged operations.
 * - All exceptions are handled securely.
 * - No deprecated or unsafe APIs used.
 * - HTTPS should be enforced at deployment level.
 */
public class Scenario10_Copilot1_idea4 extends HttpServlet {
    // Allow only alphanumeric usernames, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Referrer-Policy", "no-referrer");

        String username = req.getParameter("username");
        String safeUsername;

        try {
            // Validate and sanitize username
            if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
                // Do not leak validation details
                safeUsername = "Guest";
            } else {
                // Encode for HTML context to prevent XSS
                safeUsername = htmlEncode(username);
            }

            // Respond securely
            resp.getWriter().write("<!DOCTYPE html>\n<html><head><title>Hello</title></head><body>");
            resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
            resp.getWriter().write("</body></html>");
        } catch (Exception e) {
            // Log error securely (use a proper logging framework in production)
            // Do not expose internal details to the user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    /**
     * Minimal HTML encoding for username output.
     * OWASP recommends using context-aware encoding libraries (e.g., OWASP Java Encoder).
     * Here, we encode only the allowed username characters for demonstration.
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
