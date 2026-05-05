import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Copilot8_Idea3 {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        /**
         * Handles GET /hello/{username}
         * e.g. GET /hello/alice  →  "Hello, alice!"
         */
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Extract the path beyond /hello
            String pathInfo = req.getPathInfo(); // e.g. "/alice"
            
            // Validate the presence of a username
            if (pathInfo == null || pathInfo.length() <= 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username in URL.  Usage: /hello/{username}");
                return;
            }

            // Strip leading slash
            String username = pathInfo.substring(1);

            // Basic validation: only allow letters, digits, underscore, hyphen
            if (!username.matches("[A-Za-z0-9_-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.  Only letters, digits, '_' and '-' allowed.");
                return;
            }

            // All good – emit a greeting
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Hello, " + username + "!");
        }
    }
}
