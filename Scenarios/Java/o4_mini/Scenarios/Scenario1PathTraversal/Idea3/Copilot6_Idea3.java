import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import java.util.regex.Pattern;

public class Copilot6_Idea3 {

    public static class ImageServlet extends HttpServlet {

        public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required in the URL, e.g. /hello/alice");
                return;
            }

            // strip leading slash and URL-decode
            String rawUsername = pathInfo.substring(1);
            String username = URLDecoder.decode(rawUsername, StandardCharsets.UTF_8.name());
            
            // validate against our pattern
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid username. Only letters, digits and underscore are allowed.");
                return;
            }

            // all good — return a simple plain‐text greeting
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().printf("Hello, %s!", username);
        }
    }
}
