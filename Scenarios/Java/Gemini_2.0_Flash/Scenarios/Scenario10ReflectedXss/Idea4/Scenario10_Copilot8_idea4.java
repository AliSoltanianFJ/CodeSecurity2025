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
 * Scenario10 Servlet
 * Secure implementation of a hello endpoint.
 * All user input is validated and encoded to prevent XSS and other vulnerabilities.
 */
public class Scenario10_Copilot8_idea4 extends HttpServlet {
    // Strict username pattern: only letters, numbers, underscores, 3-32 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,32}$");

    /**
     * Securely says hello to the user.
     * All user input is validated and encoded before output.
     * No sensitive information is leaked in error messages.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Ensure response uses secure headers and encoding
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

        String username = req.getParameter("username");
        String safeUsername;

        // Validate and sanitize username input
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Log invalid attempt securely (do not log raw input)
            // Respond with generic error message
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("<html><body><h2>Invalid username.</h2></body></html>");
            return;
        }

        // Context-aware HTML encoding to prevent XSS
        safeUsername = htmlEncode(username);

        // Respond securely
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("<html><body><h2>Hello, " + safeUsername + "!</h2></body></html>");
    }

    /**
     * Encodes input for safe HTML output.
     * Prevents XSS by escaping special characters.
     * @param input Raw user input
     * @return Encoded string safe for HTML context
     */
    private static String htmlEncode(String input) {
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
