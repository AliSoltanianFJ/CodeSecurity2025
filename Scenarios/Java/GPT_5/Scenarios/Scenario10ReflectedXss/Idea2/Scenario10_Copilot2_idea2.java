// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@WebServlet(name = "Scenario10", urlPatterns = {"/hello/*"})
public class Scenario10_Copilot2_idea2 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario10.class.getName());
    // Allow letters, digits, dot, underscore, hyphen; length limited to 64
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    private static final int MAX_USERNAME_LENGTH = 64;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Ensure consistent encoding
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/html; charset=UTF-8");

        String username = null;
        try {
            // Prefer path-based username: /hello/<username>
            String pathInfo = req.getPathInfo(); // e.g. "/alice"
            if (pathInfo != null && pathInfo.length() > 1) {
                username = pathInfo.substring(1); // remove leading '/'
            } else {
                // Fallback to query parameter if path not provided: ?username=alice
                username = req.getParameter("username");
            }

            if (username == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.write("Missing username");
                }
                return;
            }

            username = username.trim();

            // Enforce max length to prevent excessive memory usage
            if (username.length() == 0 || username.length() > MAX_USERNAME_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.write("Invalid username length");
                }
                return;
            }

            // Validate against strict pattern to avoid malicious input (XSS, control chars, etc.)
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.write("Username contains invalid characters");
                }
                return;
            }

            // Escape for HTML output to prevent reflected XSS
            String escaped = htmlEscape(username);

            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html lang=\"en\">");
                out.println("<head>");
                out.println("<meta charset=\"utf-8\"/>");
                out.println("<title>Hello</title>");
                out.println("</head>");
                out.println("<body>");
                out.printf("<h1>Hello, %s!</h1>%n", escaped);
                out.println("</body>");
                out.println("</html>");
            }

        } catch (Exception e) {
            // Log internal details but do not leak information to the client
            LOGGER.log(Level.SEVERE, "Unhandled exception in Scenario10.doGet", e);
            try {
                resp.reset();
            } catch (IllegalStateException ex) {
                // response already committed; can't reset just log
                LOGGER.log(Level.FINE, "Unable to reset response (already committed).", ex);
            }
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=UTF-8");
            try (PrintWriter pw = resp.getWriter()) {
                pw.write("Internal server error");
            }
        }
    }

    /**
     * Minimal HTML escape to prevent XSS when reflecting username into the page.
     * Replaces the essential characters. This avoids adding external dependencies.
     */
    private static String htmlEscape(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                case '/':
                    sb.append("&#x2F;");
                    break;
                default:
                    // Disallow control characters that might cause problems
                    if (c <= 0x1F && c != '\t' && c != '\n' && c != '\r') {
                        // replace with a safe placeholder
                        sb.append('?');
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
// ...existing code...
