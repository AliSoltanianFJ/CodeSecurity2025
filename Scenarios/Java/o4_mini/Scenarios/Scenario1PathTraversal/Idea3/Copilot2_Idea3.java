import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.io.PrintWriter;
import javax.servlet.ServletException;

public class Copilot2_Idea3 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing.");
                return;
            }

            // strip leading slash
            String rawUsername = pathInfo.substring(1);

            // URL‐decode using UTF-8
            final String username;
            try {
                username = URLDecoder.decode(rawUsername, StandardCharsets.UTF_8.name());
            } catch (IllegalArgumentException e) {
                // thrown if the percent‐encoding is invalid
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username encoding.");
                return;
            }

            // Write a plain‐text response
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Hello, " + username + "!");            
        }
    }
}
