import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot_8_idea3 extends HttpServlet {
    
    private static final Pattern USERNAME_ALPHANUM_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");
    private static final Pattern USERNAME_UTF8_PATTERN = Pattern.compile("^([\\p{L}\\p{N}]+)$", Pattern.UNICODE_CASE | Pattern.CANON_EQ);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Check for HTTPS (defense in depth; actual enforcement should be at the server/proxy level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String pathInfo = req.getPathInfo(); // e.g., "/alice"
        String username = null;

        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing.");
            return;
        }

        // Validate and sanitize the username
        if (!isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        // Principle of least privilege: Only allow alphanumeric usernames in URL paths

        // XSS prevention: Always escape user input before rendering it
        String safeUsername = escapeForHtml(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write("");
        resp.getWriter().write("");
        resp.getWriter().write("Hello, " + safeUsername + "!");
        resp.getWriter().write("");
    }

    /**
     * Validates the username according to the application's security policy.
     */
    private boolean isValidUsername(String username) {
        // Allow either:
        // 1. Alphanumeric usernames (ASCII only)
        if (USERNAME_ALPHANUM_PATTERN.matcher(username).matches()) {
            return true;
        }
        // 2. UTF-8 names that are properly encoded and don't contain control or reserved characters
        else if (username.length() <= 50 &&
                USERNAME_UTF8_PATTERN.matcher(username).matches() &&
                !username.contains("%") && // No percent encoding in path_info, as per servlet spec
                !username.contains("/") &&
                !username.contains("\\")) {
            return true;
        }
        return false;
    }

    /**
     * Escapes special HTML characters to prevent XSS.
     */
    private String escapeForHtml(String input) {
        if (input == null) return "";
        StringBuilder escaped = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&':  escaped.append("&");  break;
                case '<':  escaped.append("<");   break;
                case '>':  escaped.append(">");   break;
                case '"':  escaped.append("\""); break;
                case '\'': escaped.append("'"); break;
                case '/':  escaped.append("/"); break;
                default:   escaped.append(c);
            }
        }
        return escaped.toString();
    }
}