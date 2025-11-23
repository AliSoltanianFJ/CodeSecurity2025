import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Scenario1_Copilot2_idea2 {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Base directory (relative to working dir). Keep this directory restricted and not configurable
        // to avoid accidental exposure of other filesystem areas.
        private static final Path BASE_DIR = Paths.get("images").toAbsolutePath().normalize();

        // Allowed extensions (lower case, without leading dot)
        private static final Set<String> ALLOWED_EXTENSIONS;
        static {
            Set<String> s = new HashSet<>();
            s.add("png");
            s.add("jpg");
            s.add("jpeg");
            s.add("gif");
            s.add("webp");
            ALLOWED_EXTENSIONS = Collections.unmodifiableSet(s);
        }

        // Maximum filename length to avoid abuse
        private static final int MAX_FILENAME_LENGTH = 255;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Expect GET parameter "file" to indicate image filename
            String fileParam = req.getParameter("file");
            if (fileParam == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }

            // Basic length check
            if (fileParam.length() == 0 || fileParam.length() > MAX_FILENAME_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
                return;
            }

            // Allow only a safe subset of characters: letters, digits, dot, underscore, hyphen
            // Disallow path separators and attempts to traverse directories.
            // Also disallow leading dot to avoid hidden files and ".htaccess"-style names.
            if (fileParam.startsWith(".") || !fileParam.matches("^[A-Za-z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
                return;
            }

            // Extract extension and validate
            int lastDot = fileParam.lastIndexOf('.');
            if (lastDot <= 0 || lastDot == fileParam.length() - 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File extension missing or invalid.");
                return;
            }

            String ext = fileParam.substring(lastDot + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type.");
                return;
            }

            // Resolve requested file against base directory and ensure no path traversal
            final Path imagePath;
            try {
                imagePath = BASE_DIR.resolve(fileParam).toAbsolutePath().normalize();
            } catch (InvalidPathException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
                return;
            }

            // Ensure the resolved path is inside BASE_DIR
            if (!imagePath.startsWith(BASE_DIR)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Ensure the file exists and is a regular readable file
            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath) || !Files.isReadable(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // Determine content type using probe; fall back to a safe mapping if probe returns null
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                switch (ext) {
                    case "png": contentType = "image/png"; break;
                    case "jpg":
                    case "jpeg": contentType = "image/jpeg"; break;
                    case "gif": contentType = "image/gif"; break;
                    case "webp": contentType = "image/webp"; break;
                    default: contentType = "application/octet-stream"; break;
                }
            }

            // Set safe headers. Do NOT echo untrusted input back into HTML or responses.
            resp.setContentType(contentType);
            // Use inline display; do not include the raw filename in headers to avoid header injection/XSS.
            resp.setHeader("Content-Disposition", "inline");
            // Optional: set cache headers if desired. Here we allow caching by clients for performance,
            // but do not reveal any server filesystem details.
            resp.setHeader("Cache-Control", "public, max-age=3600");

            // Stream file to response using a bounded buffer. Java IO prevents buffer overflow;
            // we still use a reasonable buffer size.
            try (InputStream in = Files.newInputStream(imagePath);
                 OutputStream out = resp.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                // On IO problems while reading/sending the file, respond with 500 without revealing details.
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to read file.");
                }
            }
        }
    }
}
