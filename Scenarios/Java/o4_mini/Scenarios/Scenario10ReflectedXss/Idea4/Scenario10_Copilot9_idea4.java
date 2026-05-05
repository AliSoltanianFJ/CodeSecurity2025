import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Secure Hello Servlet.
 * 
 * Security decisions:
 * - Input is strictly validated and sanitized to prevent XSS (CWE-79) and injection attacks.
 * - Output is contextually encoded for HTML.
 * - No sensitive information is leaked in error messages.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: no unnecessary permissions or operations.
 * - All exceptions are handled securely.
 * - HTTPS should be enforced at the deployment level.
 */
public class Scenario10_Copilot9_idea4 extends HttpServlet {

    // Allow only alphanumeric usernames, 3-20 chars, to minimize attack surface.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,20}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        resp.setContentType("text/html; charset=UTF-8");

        String username = req.getParameter("username");
        String safeUsername;

        // Validate and sanitize username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Do not reveal details about validation to prevent information disclosure (CWE-209)
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Error</title></head><body>");
                out.println("<h2>Invalid username.</h2>");
                out.println("</body></html>");
            }
            return;
        }

        // Contextual HTML encoding (defense-in-depth, though input is already validated)
        safeUsername = htmlEncode(username);

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Hello</title></head><body>");
            out.println("<h1>Hello, " + safeUsername + "!</h1>");
            out.println("</body></html>");
        } catch (IOException e) {
            // Log error securely (logging not shown here), do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    /**
     * Minimal HTML encoding for username output.
     * Only needed for defense-in-depth since input is strictly validated.
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
