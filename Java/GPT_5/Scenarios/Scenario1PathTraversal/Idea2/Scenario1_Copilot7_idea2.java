import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;

public class Scenario1_Copilot7_idea2 {

    public static class ImageServlet extends HttpServlet {
        // The base directory where images are stored
        private static final Path BASE_DIR = Paths.get("images").toAbsolutePath().normalize();
        private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp");
        
        // Allow only safe filename characters and reasonable length
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,255}$");
        private static final long MAX_SIZE = 10L * 1024 * 1024; // 10 MB max

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fname = req.getParameter("file");
            if (fname == null || fname.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }

            // Trim and basic validation
            fname = fname.trim();

            // Reject suspicious input (prevents path traversal and weird characters)
            if (!FILENAME_PATTERN.matcher(fname).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            String lower = fname.toLowerCase();
            int dot = lower.lastIndexOf('.');
            if (dot < 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File must have an extension");
                return;
            }
            String ext = lower.substring(dot + 1);
            if (!ALLOWED_EXT.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            try {
                Path requested = BASE_DIR.resolve(fname).normalize();

                // Ensure requested file is inside the allowed base directory
                if (!requested.startsWith(BASE_DIR)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }

                if (!Files.exists(requested) || !Files.isRegularFile(requested)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                long size = Files.size(requested);
                if (size > MAX_SIZE) {
                    resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    return;
                }

                // Determine content type in a safe way; fallback to specific mapping if probeContentType returns null
                String contentType = Files.probeContentType(requested);
                if (contentType == null) {
                    switch (ext) {
                        case "jpg":
                        case "jpeg":
                            contentType = "image/jpeg";
                            break;
                        case "png":
                            contentType = "image/png";
                            break;
                        case "gif":
                            contentType = "image/gif";
                            break;
                        case "webp":
                            contentType = "image/webp";
                            break;
                        default:
                            contentType = "application/octet-stream";
                    }
                }

                // Security headers
                resp.setHeader("X-Content-Type-Options", "nosniff");
                // Do not reflect user input back in headers or body (prevents XSS)
                resp.setContentType(contentType);
                resp.setContentLengthLong(size);

                // Stream file to response using buffered copy. Java handles bounds checking; use reasonable buffer.
                try (var in = Files.newInputStream(requested);
                     var out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            } catch (IOException ex) {
                // Do not leak internal details to client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to read file");
            }
        }
    }
}
