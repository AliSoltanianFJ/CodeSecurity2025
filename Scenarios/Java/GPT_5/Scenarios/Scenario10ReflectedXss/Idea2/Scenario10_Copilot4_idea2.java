import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Safe servlet that responds to hello/<username> or ?username=<username>.
 * - Validates and sanitizes input using a strict whitelist.
 * - Limits length to prevent resource exhaustion.
 * - Encodes output to avoid XSS.
 * - Handles all exceptions without leaking sensitive info.
 */
public class Scenario10_Copilot4_idea2 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario10.class.getName());

    // Strict whitelist: ASCII letters, digits, underscore and hyphen only.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");
    private static final int MAX_USERNAME_LENGTH = 50;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = null;

        try {
            // Prefer path-based endpoint: /hello/<username> when servlet mapped to /hello/*
            String pathInfo = req.getPathInfo(); // returns "/<username>" or null
            if (pathInfo != null && pathInfo.length() > 1) {
                // remove leading slash
                username = pathInfo.substring(1);
            } else {
                // fallback to query parameter ?username=
                username = req.getParameter("username");
            }

            String safeUsername = sanitizeAndValidate(username);

            // Use a safe content type (text/plain) and explicit UTF-8 encoding.
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);

            // Write response (safeUsername already HTML-encoded if needed)
            resp.getWriter().write("Hello, " + safeUsername + "!");
        } catch (IllegalArgumentException e) {
            // Bad input do not reveal details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error while writing response", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        } catch (Exception e) {
            // Catch-all: log and return generic error without internal details
            LOGGER.log(Level.SEVERE, "Unexpected server error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Validates and sanitizes the username input.
     * - Enforces non-null, length limits, and whitelist characters.
     * - Returns a safe, encoded representation suitable for output.
     *
     * Throws IllegalArgumentException for any invalid input.
     */
    private static String sanitizeAndValidate(String input) {
        if (input == null) {
            throw new IllegalArgumentException("username required");
        }

        String trimmed = input.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("username required");
        }

        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("username too long");
        }

        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            // Strict whitelist prevents XSS and many injection vectors.
            throw new IllegalArgumentException("username contains invalid characters");
        }

        // The username matches the whitelist (safe characters). Still perform encoding
        // to be defensive if the output context changes later.
        return escapeHtml(trimmed);
    }

    /**
     * Minimal HTML-escaping for safety if output is ever rendered as HTML.
     * Keeps the implementation small and dependency-free.
     */
    private static String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
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
}
