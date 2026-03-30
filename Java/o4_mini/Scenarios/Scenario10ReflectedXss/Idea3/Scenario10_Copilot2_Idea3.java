import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot2_Idea3 extends HttpServlet {
    private static final int MAX_USERNAME_LENGTH = 30;
    // Only allow letters, digits, underscore and hyphen in usernames
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_\\-]{1," + MAX_USERNAME_LENGTH + "}$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Extract username from the path info: /hello/<username>
        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }
        String username = pathInfo.substring(1);

        // 2. Validate username format
        if (!username.matches(USERNAME_REGEX)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Invalid username; only letters, digits, '_' and '-' up to "
                             + MAX_USERNAME_LENGTH + " characters are allowed.");
            return;
        }

        // 3. HTML-escape to prevent XSS
        String safeUsername = escapeHtml(username);

        // 4. Write response
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
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
        }
    }

    /**
     * Minimal HTML escaping.
     */
    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}