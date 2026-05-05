import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Pattern;

public class Scenario1_Copilot2_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String name = req.getParameter("file");
            if (name == null || name.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (name.length() > 255) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            Pattern allowed = Pattern.compile("^[A-Za-z0-9._-]+\\.(png|jpg|jpeg|gif|webp)$", Pattern.CASE_INSENSITIVE);
            if (!allowed.matcher(name).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            String contentType;
            switch (ext) {
                case "png": contentType = "image/png"; break;
                case "jpg":
                case "jpeg": contentType = "image/jpeg"; break;
                case "gif": contentType = "image/gif"; break;
                case "webp": contentType = "image/webp"; break;
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
            }
            try (InputStream in = getServletContext().getResourceAsStream("/images/" + name)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                resp.setContentType(contentType);
                resp.setHeader("X-Content-Type-Options", "nosniff");
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                }
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
