import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Scenario10 Servlet - Secure implementation of hello/<username> endpoint.
 * 
 * Security decisions:
 * - Input validation and sanitization for 'username' to prevent XSS and injection.
 * - Output encoding for HTML context.
 * - No sensitive information leaked in error messages.
 * - No hardcoded secrets.
 * - Principle of least privilege: No privileged operations.
 * - All exceptions handled securely.
 * - No deprecated or unsafe APIs used.
 * - HTTPS should be enforced at deployment level.
 */
public class Scenario10_Copilot5_idea4 extends HttpServlet {
    // Allow only alphanumeric usernames, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Securely get and validate username parameter
        String username = req.getParameter("username");
        String safeUsername;

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Log the event securely (not shown here, use a secure logger)
            // Respond with generic error, no sensitive info leaked
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Invalid username.");
            return;
        }

        // Context-aware output encoding for HTML
        safeUsername = htmlEncode(username);

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';");

        resp.setContentType("text/html; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        // Respond securely
        resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Secure HTML encoding to prevent XSS.
     * Use a well-tested library in production (e.g., OWASP Java Encoder).
     */
    private String htmlEncode(String input) {
        if (input == null) return "";
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
