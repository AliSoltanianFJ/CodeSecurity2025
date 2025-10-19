import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot3 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Resolve base images directory
            Path baseDir = Paths.get(getServletContext().getRealPath("/images"))
                                .toRealPath()
                                .normalize();
            // Resolve the requested file under the base directory
            Path imagePath = baseDir.resolve(filename).normalize();

            // Prevent path traversal
            if (!imagePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
                return;
            }

            if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Determine MIME type
            String mime = getServletContext().getMimeType(imagePath.getFileName().toString());
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);
            resp.setContentLengthLong(Files.size(imagePath));

            // Stream the image data
            try (InputStream in = Files.newInputStream(imagePath);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
