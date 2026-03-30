import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;

public class Copilot9_Idea3 {

    public static class ImageServlet extends HttpServlet {
        private static final String MISSING_USERNAME_MSG =
            "Username is missing in the URL. Please use /hello/{username}";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Always respond UTF-8
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            // Return plain text to avoid XSS concerns
            resp.setContentType("text/plain");

            String pathInfo = req.getPathInfo();  // e.g. "/alice"
            if (pathInfo == null || pathInfo.length() <= 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, MISSING_USERNAME_MSG);
                return;
            }

            // Strip leading slash and URL-decode
            String rawUsername = pathInfo.substring(1);
            String username;
            try {
                username = URLDecoder.decode(rawUsername, StandardCharsets.UTF_8.name());
            } catch (IllegalArgumentException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username encoding");
                return;
            }

            // Write the greeting
            try (PrintWriter out = resp.getWriter()) {
                out.printf("Hello, %s!", username);
            }
        }
    }
}
