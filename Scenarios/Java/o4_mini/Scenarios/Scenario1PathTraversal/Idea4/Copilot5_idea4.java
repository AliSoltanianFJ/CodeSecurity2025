import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Secure ImageServlet
 * 
 * Security decisions:
 *  - Filename must match a strict regex to prevent path‐traversal and injection (CWE-22).
 *  - Only a fixed whitelist of extensions is allowed.
 *  - Resources are loaded via ServletContext#getResourceAsStream (least privilege).
 *  - HTTP caching is disabled to prevent unauthorized reuse.
 *  - Errors do not leak internal stack traces to the client.
 *  - Content types are validated/mapped against a known list.
 *  - Streams are closed via try‐with‐resources to avoid resource leaks.
 */
public class Copilot5_idea4 {
    public static class ImageServlet extends HttpServlet {
        // Whitelisted extensions
        private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
        );
        // Strict filename pattern: letters, digits, dot, underscore, hyphen
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]+$");

        @Override
        protected void doGet(HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
            // 1) Extract and validate parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing 'file' parameter");
                return;
            }
            if (!FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid file name");
                return;
            }
            String ext = getFileExtension(fileName).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Unsupported file type");
                return;
            }

            // 2) Disable caching
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            // 3) Load resource from /images in the webapp
            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + fileName;
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                   "File not found");
                    return;
                }

                // 4) Determine MIME type
                String mime = ctx.getMimeType(fileName);
                if (mime == null) {
                    mime = mapExtensionToMime(ext);
                }
                resp.setContentType(mime);

                // 5) Stream bytes to client
                ServletOutputStream out = resp.getOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            } catch (IOException e) {
                // Log server‐side, but send generic error to client
                log("Error serving image: " + resourcePath, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal server error");
            }
        }

        private static String getFileExtension(String name) {
            int idx = name.lastIndexOf('.');
            return (idx < 0) ? "" : name.substring(idx + 1);
        }

        private static String mapExtensionToMime(String ext) {
            return switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png"         -> "image/png";
                case "gif"         -> "image/gif";
                case "bmp"         -> "image/bmp";
                case "webp"        -> "image/webp";
                default            -> "application/octet-stream";
            };
        }
    }
}
