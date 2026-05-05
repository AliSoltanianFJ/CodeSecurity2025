import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.io.PrintWriter;
import javax.servlet.ServletException;

public class Copilot3_Idea3 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
            // Extract the “/username” part of the path
            String pathInfo = req.getPathInfo();    // e.g. "/bob"
            if (pathInfo == null || pathInfo.length() < 2) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
                return;
            }

            // Strip leading slash
            String username = pathInfo.substring(1);

            // Basic validation: only allow letters, digits, dash or underscore
            if (!username.matches("[A-Za-z0-9_-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
                return;
            }

            // Prepare the response
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);

            // Write the greeting
            try (PrintWriter out = resp.getWriter()) {
                out.printf("Hello, %s!", username);
            }            
        }
    }
}
