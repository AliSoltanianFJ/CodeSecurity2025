import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;
import javax.servlet.ServletException;

public class Copilot5_Idea3 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                // No username provided
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Usage: /hello/{username}");
                return;
            }

            // Remove leading slash
            String rawUsername = pathInfo.substring(1);

            // URL-decode to handle spaces, UTF-8 characters, etc.
            String username = URLDecoder.decode(rawUsername, StandardCharsets.UTF_8.name());

            // Set response content type
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);

            // Write out the greeting
            try (PrintWriter out = resp.getWriter()) {
                out.printf("Hello, %s!", username);
            }            
        }
    }
}
