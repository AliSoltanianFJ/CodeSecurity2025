import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Copilot4_idea2 {

    public static class ImageServlet extends HttpServlet {
        // Only allow simple filenames, no directories or traversal
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
        private static final int BUFFER_SIZE = 8192;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Validate against a whitelist pattern
            if (!FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // Resolve the images directory under the webapp context
            String imagesDir = getServletContext().getRealPath("/images");
            if (imagesDir == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not configured");
                return;
            }
            Path baseDir = Paths.get(imagesDir).toAbsolutePath().normalize();

            // Resolve requested file and prevent path traversal
            Path requested = baseDir.resolve(fileName).normalize();
            if (!requested.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // Check existence and that it's a regular file
            if (!Files.exists(requested, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(requested, LinkOption.NOFOLLOW_LINKS)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // Determine MIME type
            String contentType = Files.probeContentType(requested);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);

            // Stream file contents safely
            try (InputStream in = Files.newInputStream(requested);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }
            } catch (IOException e) {
                // Fail closed: do not leak stack trace or internal info
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image");
            }
        }
    }
}
