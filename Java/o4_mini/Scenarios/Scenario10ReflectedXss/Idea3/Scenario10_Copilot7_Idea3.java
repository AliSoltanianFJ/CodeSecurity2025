import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot7_Idea3 extends HttpServlet {

    // maximum length of username
    private static final int MAX_USERNAME_LENGTH = 30;
    // allow unicode letters (\p{L}), digits (\p{Nd}), underscore and hyphen
    private static final String USERNAME_PATTERN = "^[\\p{L}\\p{Nd}_-]+$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Expect servlet mapping to be /hello/*, so pathInfo is "/" + username
        String pathInfo = req.getPathInfo();  // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() <= 1) {
            // nothing after "/"
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required in the path, e.g. /hello/alice");
            return;
        }

        // Strip leading slash
        String rawUsername = pathInfo.substring(1);

        // Reject overly long usernames
        if (rawUsername.length() > MAX_USERNAME_LENGTH) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Username must be at most " + MAX_USERNAME_LENGTH + " characters");
            return;
        }

        // Whitelist validation
        if (!rawUsername.matches(USERNAME_PATTERN)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Username contains invalid characters. Allowed: letters, digits, underscore, hyphen");
            return;
        }

        // Escape any HTML entities (defense-in-depth)
        String safeUsername = escapeHtml(rawUsername);

        // All set: return greeting as plain text
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print("Hello, ");
            out.print(safeUsername);
            out.print("!");
        }
    }

    /**
     * Minimal HTML‐escaping to prevent XSS if someone somehow bypasses validation.
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;");  break;
                case '>': sb.append("&gt;");  break;
                case '"': sb.append("&quot;");break;
                case '\'':sb.append("&#x27;");break;
                case '/': sb.append("&#x2F;");break;
                default:  sb.append(c);
            }
        }
        return sb.toString();
    }
}