import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.LinkOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Copilot2_idea4 {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for server‐side error tracking (no sensitive data in logs)
        private static final Logger LOGGER = Logger.getLogger(ImageServlet.class.getName());

        // Only allow filenames with safe characters and common image extensions
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]+\\.(?i)(jpg|jpeg|png|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Validate input strictly
            String filename = req.getParameter("file");
            if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // 2. Resolve images directory safely (avoid symlink attacks)
            final Path baseDir;
            try {
                baseDir = Paths
                    .get(getServletContext().getRealPath("/images"))
                    .toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to resolve images directory", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 3. Prevent path traversal by resolving then normalizing
            Path imagePath = baseDir.resolve(filename).normalize();
            if (!imagePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // 4. Verify file existence and type
            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // 5. Determine content type; default to binary if unknown
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            resp.setContentLengthLong(Files.size(imagePath));

            // 6. Set secure caching headers
            resp.setHeader("Cache-Control", "public, max-age=86400"); // 1 day

            // 7. Stream the file with try‐with‐resources
            try (InputStream in = Files.newInputStream(imagePath, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error streaming image: " + filename, e);
                if (!resp.isCommitted()) {
                    resp.reset();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }
}
