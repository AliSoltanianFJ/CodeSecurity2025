
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/hello/*")
public class Scenario10_Copilot3_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int MAX_USERNAME_LENGTH = 32;
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_-]{1," + MAX_USERNAME_LENGTH + "}$";
    private static final Logger LOGGER = Logger.getLogger(Scenario10_Copilot3_idea2.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Security headers
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';");

        String username = null;

        try {
            // Primary source: path segment after /hello/
            String pathInfo = req.getPathInfo(); // may be null or like "/alice"
            if (pathInfo != null && !pathInfo.isEmpty()) {
                // remove leading slash and take only the first segment
                String segment = pathInfo.substring(1);
                int slashIdx = segment.indexOf('/');
                if (slashIdx >= 0) {
                    segment = segment.substring(0, slashIdx);
                }
                // decode percent-encoding safely using known charset
                username = URLDecoder.decode(segment, StandardCharsets.UTF_8.name());
            }

            // Fallback: query parameter ?username=
            if ((username == null || username.isEmpty()) && req.getParameter("username") != null) {
                username = req.getParameter("username");
            }

            // Validate presence
            if (username == null || username.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Missing username");
                }
                return;
            }

            // Normalize and sanitize simple control characters (defense in depth)
            username = username.replaceAll("[\\r\\n\\t\\0\\x0B]", "");

            // Validate against allowed pattern and length
            if (username.length() > MAX_USERNAME_LENGTH || !username.matches(USERNAME_PATTERN)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Invalid username");
                }
                return;
            }

            // At this point the username contains only safe characters.
            // Use text/plain output (avoids HTML interpretation) and limit response size.
            String greeting = "Hello, " + username;

            // Write response
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter w = resp.getWriter()) {
                w.println(greeting);
            }

        } catch (IllegalArgumentException e) {
            // Could be thrown by URLDecoder.decode with unsupported encoding name or invalid input
            LOGGER.log(Level.WARNING, "Bad request data while decoding username", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Invalid username encoding");
            }
        } catch (Exception e) {
            // Generic handler: log internal details, return a generic message to the client
            LOGGER.log(Level.SEVERE, "Unhandled error in hello endpoint", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter w = resp.getWriter()) {
                w.println("An internal error occurred");
            }
        }
    }
}
// ...existing code...
