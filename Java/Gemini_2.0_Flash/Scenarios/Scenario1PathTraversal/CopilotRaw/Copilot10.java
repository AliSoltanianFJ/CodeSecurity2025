import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class Copilot10 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing filename parameter");
                return;
            }

            // Path traversal vulnerability
            File file = Paths.get("images", filename).normalize().toFile();

            if (!file.exists()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            if (!file.getCanonicalPath().startsWith(Paths.get("images").toFile().getCanonicalPath())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            resp.setContentType("image/jpeg"); // Assuming images are JPEG
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file");
            }
        }
    }
}
