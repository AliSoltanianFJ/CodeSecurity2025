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
 * - Input is strictly validated and sanitized to prevent XSS and injection (CWE-20, CWE-79).
 * - Output is contextually encoded for HTML (CWE-116).
 * - No sensitive information is leaked in error messages (CWE-209).
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: no database or file access.
 * - HTTPS should be enforced at the server/container level.
 */
public class Scenario10_Copilot5_idea4 extends HttpServlet {
    // Allow only alphanumeric usernames, 3-20 chars, to prevent XSS and injection.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,20}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Set secure headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        // Ensure cookies are secure and HttpOnly at the container level

        String username = req.getParameter("username");
        String safeUsername;

        // Validate and sanitize input
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Log the event securely (not shown here)
            safeUsername = "Guest";
        } else {
            safeUsername = username;
        }

        // Contextual HTML encoding (defense-in-depth, though input is validated)
        safeUsername = htmlEncode(safeUsername);

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("  <meta charset=\"UTF-8\">");
            out.println("  <title>Hello</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("  <h1>Hello, " + safeUsername + "!</h1>");
            out.println("</body>");
            out.println("</html>");
        } catch (IOException e) {
            // Do not leak sensitive info to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
            // Log the exception securely (not shown here)
        }
    }

    /**
     * Minimal HTML encoding for output context.
     * In production, use a well-maintained library such as OWASP Java Encoder.
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
