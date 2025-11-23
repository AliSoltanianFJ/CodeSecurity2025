// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class Scenario1_Copilot6_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String name = req.getParameter("file");
            if (name == null || name.isEmpty() || name.length() > 255) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (!name.matches("[A-Za-z0-9._-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp"))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            ServletContext ctx = getServletContext();
            resp.setHeader("X-Content-Type-Options", "nosniff");
            try (InputStream is = ctx.getResourceAsStream("/images/" + name)) {
                if (is == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                String mime = ctx.getMimeType(name);
                if (mime == null) mime = "application/octet-stream";
                resp.setContentType(mime);
                try (ServletOutputStream os = resp.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = is.read(buf)) != -1) {
                        os.write(buf, 0, r);
                    }
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
// ...existing code...
