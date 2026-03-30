import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;

@WebServlet(name = "HelloServlet", urlPatterns = {"/hello/*"})
public class Scenario10_Copilot8_Idea3 extends HttpServlet {
    private static final int MAX_USERNAME_LENGTH = 50;
    // only letters, digits, space, hyphen, underscore allowed
    private static final String USERNAME_REGEX = "^[A-Za-z0-9 _\\-]+$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) Extract the “username” from path info
        //    e.g. GET /hello/alice  ⇒  pathInfo="/alice"
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required in the URL: /hello/{username}");
            return;
        }

        // 2) URL-decode (to allow e.g. URL-encoded spaces, non-ASCII)
        String raw = pathInfo.substring(1); // strip leading '/'
        String username;
        try {
            username = URLDecoder.decode(raw, "UTF-8");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL encoding in username");
            return;
        }

        // 3) Validate length and allowed characters
        if (username.length() > MAX_USERNAME_LENGTH || !username.matches(USERNAME_REGEX)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Username must be 1–" + MAX_USERNAME_LENGTH +
                    " chars long and only contain letters, digits, space, '-' or '_'.");
            return;
        }

        // 4) Escape for HTML to prevent XSS
        String safeUsername = htmlEscape(username);

        // 5) Build response
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("  <meta charset=\"UTF-8\">");
            out.println("  <title>Hello, " + safeUsername + "!</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("  <h1>Hello, " + safeUsername + "!</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Very simple HTML escaper. Replaces the five special chars
     * &, <, >, " and '.
     */
    private static String htmlEscape(String s) {
        if (s == null) return null;
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