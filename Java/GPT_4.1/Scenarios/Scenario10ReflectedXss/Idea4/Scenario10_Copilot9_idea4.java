import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// OWASP: Use context-aware output encoding to prevent XSS
import org.apache.commons.text.StringEscapeUtils;

public class Scenario10_Copilot9_idea4 extends HttpServlet {
    // Username validation: only allow alphanumeric and limited safe characters
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-\\.]{1,32}$");

    /**
     * Securely says hello to the user at the /hello/<username> endpoint.
     * All user input is validated and encoded to prevent XSS and other attacks.
     * No sensitive information is leaked in error messages.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense in depth, but actual enforcement should be at server config)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        // Extract username from path info, not query string, for RESTful endpoint
        String pathInfo = req.getPathInfo(); // e.g., "/john"
        String username = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1); // Remove leading '/'
        }

        // Validate username strictly
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Do not reveal validation details to user
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Invalid username.");
            return;
        }

        // Context-aware output encoding to prevent XSS
        String safeUsername = StringEscapeUtils.escapeHtml4(username);

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self'; font-src 'self'; img-src 'self'; script-src 'none';");
        resp.setHeader("Referrer-Policy", "no-referrer");

        // Use UTF-8 for all output
        resp.setContentType("text/html; charset=UTF-8");

        // Respond with a safe hello message
        resp.getWriter().write("<!DOCTYPE html>\n<html lang=\"en\">\n<head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }
}
