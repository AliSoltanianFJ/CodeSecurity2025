import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot10_idea2 extends HttpServlet {
    // Regex for a safe username: only letters, numbers, underscores, 3-20 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String safeUsername;

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            // Invalid or missing username
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Invalid username.");
            return;
        } else {
            // Sanitize username for output (defense in depth)
            safeUsername = org.apache.commons.text.StringEscapeUtils.escapeHtml4(username);
        }

        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().write("Hello, " + safeUsername + "!");
    }
}
