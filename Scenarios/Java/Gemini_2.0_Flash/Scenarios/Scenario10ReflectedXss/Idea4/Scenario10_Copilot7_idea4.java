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

// Security Decisions:
// - Input is strictly validated and sanitized to prevent XSS and injection attacks.
// - Output is contextually encoded for HTML.
// - No sensitive information is leaked in error messages.
// - No secrets or credentials are hardcoded.
// - Principle of least privilege: no privileged operations performed.
// - HTTPS should be enforced at the deployment level.

public class Scenario10_Copilot7_idea4 extends HttpServlet {
    // Strict username pattern: only allow alphanumeric and underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    /**
     * Securely says hello to the user at the endpoint hello/<username>.
     * All user input is validated and encoded to prevent XSS and injection attacks.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce secure response headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        String username = req.getParameter("username");
        String safeUsername = "Guest"; // Default fallback

        if (username != null) {
            // Validate username strictly
            Matcher matcher = USERNAME_PATTERN.matcher(username);
            if (matcher.matches()) {
                // Encode for HTML context to prevent XSS
                safeUsername = htmlEncode(username);
            } else {
                // Log validation failure securely (do not expose to user)
                // In production, use a secure logging framework
                // System.err.println("Invalid username attempted: " + username);
                safeUsername = "Guest";
            }
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        // Respond with a safe greeting
        resp.getWriter().write("<!DOCTYPE html>\n<html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Context-aware HTML encoder to prevent XSS.
     * Uses minimal encoding for demonstration; use Apache Commons Text or OWASP Java Encoder in production.
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
