import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;
import javax.servlet.ServletException;

public class Copilot4_Idea3 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
            // Extract the part after "/hello/"
            String pathInfo = req.getPathInfo();     // e.g. "/alice"
            if (pathInfo == null || pathInfo.length() <= 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
                return;
            }

            String username = pathInfo.substring(1); // strip leading '/'
            // Simple validation: only letters, digits, underscore or hyphen
            if (!username.matches("[A-Za-z0-9_-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
                return;
            }

            // Everything’s OK → send greeting
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.print("Hello, " + username + "!");
            }            
        }
    }
}
