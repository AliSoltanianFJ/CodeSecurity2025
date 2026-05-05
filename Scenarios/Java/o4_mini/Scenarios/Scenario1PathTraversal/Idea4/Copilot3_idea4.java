import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Copilot3_idea4 {

    public static class ImageServlet extends HttpServlet {
        // Strict filename pattern: only alphanumerics, dash, underscore and allowed image extensions
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]+\\.(?i)(jpg|jpeg|png|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Enforce HTTPS; do not leak sensitive data
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS Required");
                return;
            }

            String filename = req.getParameter("file");
            if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
                // Invalid or missing filename
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image request");
                return;
            }

            // Resolve images directory in a secure, canonical way
            String imagesDir = getServletContext().getRealPath("/images");
            if (imagesDir == null) {
                // Fallback or misconfiguration
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image store not available");
                return;
            }
            Path basePath = Paths.get(imagesDir).toAbsolutePath().normalize();
            Path target = basePath.resolve(filename).normalize();

            // Prevent path traversal: ensure the resolved path is within the images directory
            if (!target.startsWith(basePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!Files.exists(target) || !Files.isRegularFile(target) || !Files.isReadable(target)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Determine MIME type safely
            String mime = getServletContext().getMimeType(target.getFileName().toString());
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);
            resp.setContentLengthLong(Files.size(target));

            // Stream file contents; use try-with-resources to ensure closure
            try (InputStream in = Files.newInputStream(target);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                // Log internally (omitted here) but do not expose stack trace
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to serve image");
            }
        }
    }
}
