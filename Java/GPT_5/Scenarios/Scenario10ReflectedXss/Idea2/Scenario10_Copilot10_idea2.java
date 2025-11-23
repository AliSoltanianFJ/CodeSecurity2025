// ...existing code...
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot10_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Whitelist allowed username characters and enforce a reasonable max length
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");
    private static final int MAX_USERNAME_LENGTH = 64;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            // Prefer path info for endpoints mapped like /hello/<username>
            // Example: if servlet is mapped to /hello/* and request is /hello/alice
            // getPathInfo() -> "/alice"
            String username = null;
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                username = pathInfo.substring(1); // trim leading '/'
            } else {
                // Fallback to query parameter
                username = req.getParameter("username");
            }

            if (username == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username");
                return;
            }

            // Normalize and validate length
            username = username.trim();
            if (username.isEmpty() || username.length() > MAX_USERNAME_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username length");
                return;
            }

            // Validate against whitelist to prevent XSS and other injection attacks
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username characters");
                return;
            }

            // Use plain text response and explicit UTF-8 charset to avoid HTML rendering
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

            // Build response with bounded buffer to avoid unexpected memory growth
            StringBuilder out = new StringBuilder(16 + username.length());
            out.append("Hello, ");
            out.append(username);
            out.append("!");

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(out.toString());
            resp.getWriter().flush();
        } catch (Exception e) {
            // Server-side logging without exposing internal details to the client
            log("Error processing hello request", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}
// ...existing code...
