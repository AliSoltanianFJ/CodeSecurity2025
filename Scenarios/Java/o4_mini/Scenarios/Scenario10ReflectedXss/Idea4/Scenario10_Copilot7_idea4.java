import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Security-focused implementation: 
// - All user input is validated and sanitized
// - Output is contextually encoded to prevent XSS
// - No sensitive information is leaked in error messages
// - Principle of least privilege: no unnecessary permissions or operations
// - No hardcoded secrets or credentials
// - Secure defaults and secure coding guidelines followed

public class Scenario10_Copilot7_idea4 extends HttpServlet {
    // Allow only alphanumeric usernames, 3-20 chars, to prevent injection/XSS
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9]{3,20}$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense-in-depth, actual enforcement should be at server config)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = req.getParameter("username");
        if (username == null || !username.matches(USERNAME_PATTERN)) {
            // Do not reveal details to avoid information disclosure
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Context-aware output encoding to prevent XSS
        String safeUsername = htmlEscape(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head><meta charset=\"UTF-8\"><title>Hello</title></head>");
            out.println("<body>");
            out.println("<h1>Hello, " + safeUsername + "!</h1>");
            out.println("</body></html>");
        } catch (IOException e) {
            // Log error securely (not shown here), do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred.");
        }
    }

    /**
     * Minimal HTML escape to prevent XSS in output.
     * In production, use a well-maintained library such as OWASP Java Encoder.
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
