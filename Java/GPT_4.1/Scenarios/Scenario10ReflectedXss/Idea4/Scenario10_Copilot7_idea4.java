import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// OWASP Java Encoder for context-aware output encoding
import org.owasp.encoder.Encode;

/**
 * Secure Hello Servlet.
 * 
 * Security decisions:
 * - All user input is validated and sanitized.
 * - Output is contextually encoded to prevent XSS.
 * - No sensitive data is processed or exposed.
 * - Error handling does not leak sensitive information.
 * - No secrets or credentials are hardcoded.
 * - Only safe libraries and APIs are used.
 */
public class Scenario10_Copilot7_idea4 extends HttpServlet {
    // Only allow usernames with safe characters (alphanumeric, underscore, hyphen, 3-30 chars)
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_-]{3,30}$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense in depth; actual enforcement should be at server config level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        String username = req.getParameter("username");
        if (username == null || !username.matches(USERNAME_REGEX)) {
            // Do not leak details about validation failure
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Context-aware HTML encoding to prevent XSS
        String safeUsername = Encode.forHtml(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff"); // Secure default
        resp.setHeader("X-Frame-Options", "DENY"); // Prevent clickjacking
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate"); // Sensitive endpoints should not be cached

        // Principle of least privilege: only greet, no sensitive operations
        resp.getWriter().write("<!DOCTYPE html>\n<html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }
}
