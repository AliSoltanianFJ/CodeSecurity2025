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

// Security-focused implementation: 
// - Validates and sanitizes all user input
// - Uses context-aware output encoding to prevent XSS
// - Handles errors securely without leaking sensitive info
// - No hardcoded secrets or credentials
// - Secure defaults and maintainable code

public class Scenario10_Copilot4_idea4 extends HttpServlet {
    // Strict username validation: only allow alphanumeric, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense in depth, actual enforcement should be at server config level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        String username = req.getParameter("username");
        if (username == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username parameter.");
            return;
        }

        // Validate username strictly
        Matcher matcher = USERNAME_PATTERN.matcher(username);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        // Context-aware output encoding to prevent XSS
        String safeUsername = htmlEncode(username);

        // Set secure headers
        resp.setHeader("Content-Type", "text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self';");

        // Respond securely
        resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Context-aware HTML encoding to prevent XSS.
     * OWASP recommendation: encode all untrusted data before outputting to HTML.
     */
    private static String htmlEncode(String input) {
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
