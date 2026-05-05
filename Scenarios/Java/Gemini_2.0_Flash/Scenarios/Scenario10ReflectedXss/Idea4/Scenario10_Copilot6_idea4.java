import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// OWASP Java Encoder library for context-aware output encoding
import org.owasp.encoder.Encode;

public class Scenario10_Copilot6_idea4 extends HttpServlet {
    /**
     * Securely says hello to the user.
     * - All user input is validated and encoded to prevent XSS (CWE-79).
     * - No sensitive information is leaked in error messages.
     * - No secrets or credentials are hardcoded.
     * - Principle of least privilege: no unnecessary permissions.
     * - All exceptions are handled securely.
     * - Use HTTPS in deployment for secure transport.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        resp.setContentType("text/html; charset=UTF-8");

        String username = req.getParameter("username");
        String safeUsername;

        try {
            // Validate username: allow only letters, numbers, underscores, and hyphens, 3-30 chars
            if (username == null || !username.matches("^[A-Za-z0-9_-]{3,30}$")) {
                // Log validation failure securely (do not expose details to user)
                safeUsername = "Guest";
            } else {
                // Context-aware HTML encoding to prevent XSS
                safeUsername = Encode.forHtml(username);
            }

            // Respond securely
            resp.getWriter().write("<!DOCTYPE html>");
            resp.getWriter().write("<html lang=\"en\"><head><title>Hello</title></head><body>");
            resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
            resp.getWriter().write("</body></html>");
        } catch (Exception e) {
            // Secure error handling: log error, generic message to user
            // In production, use a secure logger (not System.err)
            System.err.println("Error in Scenario10.doGet: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
}
