import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

// Security-relevant decisions:
// - All user input is validated and sanitized using a strict regex (alphanumeric, 3-30 chars).
// - Output is HTML-encoded to prevent XSS.
// - No sensitive information is leaked in error messages.
// - No secrets or credentials are hardcoded.
// - Principle of least privilege: no privileged operations performed.
// - HTTPS should be enforced at the server/container configuration level.

public class Scenario10_Copilot9_idea4 extends HttpServlet {
    // Strict username pattern: only letters and numbers, 3-30 characters
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,30}$");

    /**
     * Securely says hello to the user.
     * Validates and sanitizes input, encodes output to prevent XSS.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String safeUsername = "Guest"; // Default fallback

        if (username != null) {
            Matcher matcher = USERNAME_PATTERN.matcher(username);
            if (matcher.matches()) {
                // HTML-encode username to prevent XSS
                safeUsername = htmlEncode(username);
            }
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff"); // Secure header

        // Principle of least privilege: only minimal info returned
        resp.getWriter().write("<!DOCTYPE html><html><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Context-aware HTML encoding to prevent XSS.
     * @param input Raw user input
     * @return Encoded string safe for HTML context
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
