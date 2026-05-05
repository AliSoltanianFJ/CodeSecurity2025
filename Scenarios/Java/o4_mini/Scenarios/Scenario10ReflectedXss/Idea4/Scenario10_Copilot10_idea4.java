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

public class Scenario10_Copilot10_idea4 extends HttpServlet {
    // Allow only alphanumeric usernames between 3 and 30 characters
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense in depth, actual enforcement should be at server/proxy config)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String pathInfo = req.getPathInfo(); // e.g., "/hello/username"
        String username = null;

        // Extract username from path, expecting /hello/<username>
        if (pathInfo != null && pathInfo.startsWith("/hello/")) {
            username = pathInfo.substring("/hello/".length());
        }

        // Validate username strictly
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Do not reveal details about validation to avoid information disclosure
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
            return;
        }

        // Set secure headers to prevent XSS and clickjacking
        resp.setHeader("Content-Type", "text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        // Context-aware output encoding to prevent XSS
        String safeUsername = htmlEscape(username);

        // Respond with a safe hello message
        resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Minimal HTML escape to prevent XSS.
     * In production, use a well-tested library such as OWASP Java Encoder.
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
