// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
// ...existing code...

public class Scenario1_Copilot4_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Validate parameter name and content
            final String paramName = "file";
            String filename = req.getParameter(paramName);
            if (filename == null) {
                // Do not reveal internal details; return a generic bad request
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter.");
                return;
            }

            filename = filename.trim();
            if (filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Allow only safe filename characters (no path separators) to prevent path traversal
            final Pattern FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
            if (!FILENAME_PATTERN.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Restrict to a safe set of image extensions (avoid SVG to reduce XSS risk)
            final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
            String ext = "";
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
            if (!ALLOWED_EXT.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported image type.");
                return;
            }

            // Build a path relative to the web application's "images" resource directory.
            // Using ServletContext#getResourceAsStream prevents direct filesystem path manipulation.
            String resourcePath = "/images/" + filename;

            try (InputStream in = getServletContext().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // Determine MIME type and ensure it's an image type
                String mime = getServletContext().getMimeType(filename);
                if (mime == null) {
                    // Fallback to a safe default
                    mime = "application/octet-stream";
                }
                if (!mime.startsWith("image/")) {
                    // Refuse to serve non-image resources from this endpoint
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden.");
                    return;
                }

                // Security headers
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("Cache-Control", "private, max-age=3600");
                resp.setContentType(mime);

                // Stream the file out with a reasonable buffer size. Use try-with-resources to avoid leaks.
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192]; // 8 KiB buffer
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            } catch (IOException e) {
                // Log server-side, but do not expose internal details to the client
                this.getServletContext().log("Failed to serve image: " + filename, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to retrieve image.");
            }
        }
    }
}
