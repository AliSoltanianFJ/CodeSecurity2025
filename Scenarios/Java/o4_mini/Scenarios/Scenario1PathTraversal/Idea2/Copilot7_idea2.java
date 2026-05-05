import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot7_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Validate input parameter
            String filename = req.getParameter("file");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty file parameter");
                return;
            }

            // 2. Normalize filename and enforce a strict whitelist of extensions
            String normalized = Paths.get(filename).getFileName().toString(); // strip any path
            String lower = normalized.toLowerCase();
            if (!lower.matches("[a-z0-9_\\-]+\\.(png|jpe?g|gif|bmp)")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // 3. Resolve the images directory on disk
            ServletContext context = getServletContext();
            String imagesRealPath = context.getRealPath("/images");
            if (imagesRealPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory inaccessible");
                return;
            }
            Path imagesDir = Paths.get(imagesRealPath).toAbsolutePath().normalize();
            Path requested = imagesDir.resolve(normalized).normalize();

            // 4. Prevent path traversal
            if (!requested.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // 5. Check existence & that it’s not a directory
            if (Files.notExists(requested) || Files.isDirectory(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // 6. Determine MIME type
            String mimeType = context.getMimeType(requested.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requested));

            // 7. Prevent client-side caching of potentially sensitive data
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            // 8. Stream the file in a buffered way
            try (InputStream in = Files.newInputStream(requested);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // On any I/O error, send a generic 500
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
