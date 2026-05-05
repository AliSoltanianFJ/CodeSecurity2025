import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario10_Copilot4_Idea3 extends HttpServlet {
    // Allow only letters, digits and underscores in the username
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Try to extract username from path: /hello/<username>
        // If pathInfo is null or "/", fall back to ?username= parameter
        String username = null;
        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        if (pathInfo != null && pathInfo.length() > 1) {
            // strip leading '/'
            username = pathInfo.substring(1);
        } else {
            username = req.getParameter("username");
        }

        // Validate presence
        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        // Validate characters
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid username; only letters, digits and underscore (_) are allowed");
            return;
        }

        // All good: send plain-text greeting
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain; charset=UTF-8");
        // Because contentType is text/plain and we validated username strictly,
        // there’s no risk of HTML or script injection here.
        resp.getWriter().printf("Hello, %s!", username);
    }
}