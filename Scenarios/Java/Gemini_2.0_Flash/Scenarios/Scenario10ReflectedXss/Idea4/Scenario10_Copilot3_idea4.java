import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

/**
 * Scenario10 Servlet: Securely greets the user at the /hello/<username> endpoint.
 * 
 * Security decisions:
 * - Input validation: Only allows alphanumeric usernames (max 30 chars).
 * - Output encoding: Escapes HTML to prevent XSS.
 * - Error handling: Does not leak sensitive info.
 * - Principle of least privilege: No privileged operations.
 * - No secrets or credentials hardcoded.
 * - All exceptions handled.
 * - HTTPS should be enforced at deployment (not in code).
 */
public class Scenario10_Copilot3_idea4 extends HttpServlet {
    // Strict username pattern: alphanumeric, 1-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,30}$");

    /**
     * Escapes HTML special characters to prevent XSS.
     * @param input User-supplied string
     * @return Safe string for HTML output
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
        resp.setHeader("Content-Security-Policy", "default-src 'self'");

        String username = req.getParameter("username");
        String safeUsername;

        // Validate and sanitize username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            safeUsername = "Guest";
        } else {
            safeUsername = escapeHtml(username);
        }

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Hello</title></head><body>");
            out.println("<h1>Hello, " + safeUsername + "!</h1>");
            out.println("</body></html>");
        } catch (Exception e) {
            // Log error securely (use a proper logger in production)
            // Do not expose details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
}
