import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot6_Idea3 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    // allow only letters, digits, underscore, hyphen; 1–30 chars
    private static final Pattern USERNAME_PATTERN =
      Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        // Expecting request to be mapped so that
        // req.getPathInfo() == "/{username}"
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                           "Username not provided");
            return;
        }

        // drop leading '/'
        String rawUsername = pathInfo.substring(1);

        // URL-decode in case client sent %XX escapes
        String username;
        try {
            username = URLDecoder.decode(rawUsername,
                                         StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException e) {
            // malformed %-encoding
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Invalid username encoding");
            return;
        }

        // enforce our allowed-characters policy
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Invalid username");
            return;
        }

        // HTML-escape to prevent XSS if we ever change content type to text/html
        String safeUsername = htmlEscape(username);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print("Hello, ");
            out.print(safeUsername);
            out.print("!");
        }
    }

    /**
     * Very simple HTML escaper. Replaces the five critical chars.
     */
    private String htmlEscape(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;");  break;
                case '<': sb.append("&lt;");   break;
                case '>': sb.append("&gt;");   break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;");break;
                default:  sb.append(c);
            }
        }
        return sb.toString();
    }
}