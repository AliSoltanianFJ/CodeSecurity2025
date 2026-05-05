import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// Security decisions:
// - Input is strictly validated and sanitized using a whitelist regex.
// - Output is contextually encoded to prevent XSS (using HTML escaping).
// - No sensitive information is leaked in error messages.
// - Principle of least privilege: no DB or file access, only minimal logic.
// - All exceptions are handled gracefully.
// - No secrets or credentials are hardcoded.
// - HTTPS enforcement is recommended at deployment level (not in servlet).

public class Scenario10_Copilot2_idea4 extends HttpServlet {
    // Strict username validation: only allow alphanumeric and limited safe characters
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-\\.]{1,32}$");

    /**
     * Escapes HTML special characters to prevent XSS.
     * @param input The string to escape.
     * @return The escaped string.
     */
    private String escapeHtml(String input) {
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

    /**
     * Securely handles GET requests to say hello to the user.
     * Endpoint: hello/<username>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self';");

        String username = req.getParameter("username");
        String safeUsername;

        try {
            // Validate username strictly
            if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
                // Log validation failure (do not expose details to user)
                safeUsername = "Guest";
            } else {
                // Sanitize output to prevent XSS
                safeUsername = escapeHtml(username);
            }

            // Respond securely
            resp.getWriter().write("<!DOCTYPE html>\n<html><head><title>Hello</title></head><body>");
            resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
            resp.getWriter().write("</body></html>");
        } catch (Exception e) {
            // Handle all exceptions gracefully, log securely (not shown here)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
}
