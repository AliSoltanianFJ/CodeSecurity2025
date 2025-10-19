import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Securely map this servlet to /hello/*
@WebServlet("/hello/*")
public class Scenario10_Copilot4_idea4 extends HttpServlet {
    // Allow only safe usernames: letters, numbers, underscores, 3-32 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,32}$");

    /**
     * Securely says hello to the user.
     * - Validates and sanitizes the username from the path.
     * - Uses context-aware HTML encoding to prevent XSS.
     * - Does not leak sensitive information in error messages.
     * - Assumes HTTPS is enforced at the server/container level.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract username from the path info: /hello/<username>
        String pathInfo = req.getPathInfo();
        String username = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1); // Remove leading '/'
        }

        // Validate username strictly
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Log the event securely (not shown here), but do not reveal details to user
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Invalid username.");
            return;
        }

        // Context-aware HTML encoding to prevent XSS
        String safeUsername = htmlEncode(username);

        // Set secure headers
        resp.setHeader("Content-Security-Policy", "default-src 'self'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setContentType("text/html; charset=UTF-8");

        // Respond with a safe hello message
        resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Minimal HTML encoding to prevent XSS.
     * In production, use a well-maintained library such as OWASP Java Encoder.
     */
    private static String htmlEncode(String input) {
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
