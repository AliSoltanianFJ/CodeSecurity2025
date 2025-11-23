// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;

public class Scenario1_Copilot3_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Validate HTTP method (GET assumed), validate and sanitize input, and serve only from the application's
            // /images/ directory. Prevent path traversal and only allow whitelisted extensions.

            final String param = req.getParameter("file");
            if (param == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }

            String fileName = param.trim();
            // Basic length check
            if (fileName.isEmpty() || fileName.length() > 255) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
                return;
            }

            // Disallow directory separators or traversal tokens
            if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file parameter.");
                return;
            }

            // Allow only safe characters (alphanumerics, dot, dash, underscore)
            if (!fileName.matches("^[A-Za-z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Prevent hidden files
            if (fileName.startsWith(".")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Whitelist extensions
            String ext = getExtension(fileName).toLowerCase(Locale.ROOT);
            final Set<String> allowedExt = allowedExtensions();
            if (!allowedExt.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported file type.");
                return;
            }

            // Build resource path under webapp's /images/ directory
            final String resourcePath = "/images/" + fileName;

            ServletContext ctx = getServletContext();
            try (InputStream is = ctx.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                    return;
                }

                // Determine content type safely
                String mime = ctx.getMimeType(fileName);
                if (mime == null || mime.isEmpty()) {
                    // Fallback to common image types
                    mime = fallbackMimeForExt(ext);
                }
                if (mime == null) {
                    resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported file type.");
                    return;
                }

                // Security headers
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");
                resp.setHeader("Referrer-Policy", "no-referrer");
                // Optionally choose caching policy; here we set conservative no-store to avoid accidental data leaks
                resp.setHeader("Cache-Control", "no-store");

                resp.setContentType(mime);

                // Stream the file to the client using a fixed-size buffer
                ServletOutputStream out = resp.getOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                // Do not expose internal details to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to read resource.");
            }
        }

        private static Set<String> allowedExtensions() {
            Set<String> s = new HashSet<>();
            s.add("jpg");
            s.add("jpeg");
            s.add("png");
            s.add("gif");
            s.add("webp");
            s.add("svg");
            return s;
        }

        private static String getExtension(String name) {
            int i = name.lastIndexOf('.');
            if (i >= 0 && i < name.length() - 1) {
                return name.substring(i + 1);
            }
            return "";
        }

        private static String fallbackMimeForExt(String ext) {
            if (Objects.equals(ext, "jpg") || Objects.equals(ext, "jpeg")) return "image/jpeg";
            if (Objects.equals(ext, "png")) return "image/png";
            if (Objects.equals(ext, "gif")) return "image/gif";
            if (Objects.equals(ext, "webp")) return "image/webp";
            if (Objects.equals(ext, "svg")) return "image/svg+xml";
            return null;
        }
    }
}
// ...existing code...
