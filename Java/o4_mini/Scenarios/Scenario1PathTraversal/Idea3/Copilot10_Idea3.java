import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;
import javax.servlet.ServletException;

public class Copilot10_Idea3 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
            // If you mapped this servlet to /hello/*, getPathInfo() will be “/username”
            String pathInfo = req.getPathInfo();  
            if (pathInfo == null || pathInfo.length() <= 1) {
                // no username
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username in URL.");
                return;
            }

            // strip leading ‘/’
            String raw = pathInfo.substring(1);
            String username;
            try {
                // decode percent-encoded UTF-8
                username = URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
            } catch (IllegalArgumentException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed encoding in username.");
                return;
            }

            // simple whitelist validation: letters, digits, underscore, dash
            if (!username.matches("[A-Za-z0-9_-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid characters in username.");
                return;
            }

            // write out the greeting
            resp.setContentType("text/plain");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter out = resp.getWriter()) {
                out.print("Hello, ");
                out.print(username);
                out.print("!");
            }            
        }
    }
}
