import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Scenario1_Copilot8_idea4 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String file = req.getParameter("file");
            if (file == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (file.length() > 255) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (!file.matches("^(?!\\.)[A-Za-z0-9._-]{1,255}$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            int idx = file.lastIndexOf('.');
            if (idx <= 0 || idx == file.length() - 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String ext = file.substring(idx + 1).toLowerCase();
            Set<String> allowedExt = new HashSet<>(Arrays.asList(
                "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "ico"
            ));
            if (!allowedExt.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String resourcePath = "/images/" + file;
            try (InputStream in = getServletContext().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                String mime = getServletContext().getMimeType(file);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);
                resp.setHeader("X-Content-Type-Options", "nosniff");

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, read);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
