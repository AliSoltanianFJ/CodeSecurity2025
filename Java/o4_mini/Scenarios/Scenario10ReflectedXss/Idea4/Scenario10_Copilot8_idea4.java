import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Security-focused implementation: 
// - Validates and sanitizes all user input
// - Uses context-aware output encoding to prevent XSS
// - Avoids leaking sensitive information in error messages
// - Follows secure coding guidelines (OWASP, CERT)
// - No secrets or credentials are hardcoded

public class Scenario10_Copilot8_idea4 extends HttpServlet {
    // Strict username pattern: only allows alphanumeric and underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense in depth, should also be enforced at server/proxy level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        String pathInfo = req.getPathInfo(); // e.g., "/hello/username"
        String username = null;

        // Extract username from path: /hello/<username>
        if (pathInfo != null && pathInfo.startsWith("/hello/")) {
            username = pathInfo.substring("/hello/".length());
        }

        // Validate username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Do not reveal details about validation failure
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Set secure headers
        resp.setHeader("Content-Type", "text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        // Context-aware output encoding to prevent XSS
        String safeUsername = htmlEscape(username);

        // Respond with a safe greeting
        resp.getWriter().write("<!DOCTYPE html>\n<html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Escapes HTML special characters to prevent XSS.
     * Use a well-tested library (e.g., Apache Commons Text) in production.
     */
    private static String htmlEscape(String input) {
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
}
