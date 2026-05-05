import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

/**
 * Scenario10 Servlet
 * Secure implementation: avoids XSS, injection, and other common vulnerabilities.
 */
public class Scenario10_Copilot2_idea4 extends HttpServlet {
    // Strict username validation: only allow alphanumeric, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,30}$");

    /**
     * Securely says hello to the user.
     * All user input is validated and encoded before output.
     * No sensitive information is leaked in error messages.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set secure response headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        String username = req.getParameter("username");
        PrintWriter out = resp.getWriter();

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Do not reveal validation details to user
            out.println("<html><body>");
            out.println("<h2>Invalid username.</h2>");
            out.println("</body></html>");
            return;
        }

        // Context-aware HTML encoding to prevent XSS
        String safeUsername = htmlEncode(username);

        out.println("<html><body>");
        out.println("<h2>Hello, " + safeUsername + "!</h2>");
        out.println("</body></html>");
    }

    /**
     * Minimal HTML encoding for output context.
     * OWASP recommends using a library, but for demonstration, this covers basic cases.
     * @param input Raw user input
     * @return Encoded string safe for HTML context
     */
    private String htmlEncode(String input) {
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
