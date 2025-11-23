// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class Scenario1_Copilot8_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Validate presence
            String fileParam = req.getParameter("file");
            if (fileParam == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter.");
                return;
            }

            // Basic trimming and reject suspicious input
            String filename = fileParam.trim();
            if (filename.isEmpty() || filename.length() > 255) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Reject any path separators or traversal attempts
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.startsWith(".")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Validate characters (allow letters, digits, dot, underscore, hyphen)
            if (!filename.matches("^[A-Za-z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Validate extension against whitelist
            int dot = filename.lastIndexOf('.');
            if (dot < 0 || dot == filename.length() - 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File must have a valid extension.");
                return;
            }
            String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            Set<String> allowed = new HashSet<>();
            allowed.add("png");
            allowed.add("jpg");
            allowed.add("jpeg");
            allowed.add("gif");
            allowed.add("webp");
            allowed.add("bmp");

            if (!allowed.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type.");
                return;
            }

            // Use ServletContext resource path (inside webapp) to avoid filesystem traversal
            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            // Determine MIME type and ensure it's an image type
            String mime = ctx.getMimeType(filename);
            if (mime == null) {
                // Fallback mapping for common image types
                switch (ext) {
                    case "png": mime = "image/png"; break;
                    case "jpg":
                    case "jpeg": mime = "image/jpeg"; break;
                    case "gif": mime = "image/gif"; break;
                    case "webp": mime = "image/webp"; break;
                    case "bmp": mime = "image/bmp"; break;
                    default: mime = "application/octet-stream";
                }
            }
            if (!mime.startsWith("image/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Requested resource is not an image.");
                return;
            }

            // Open resource as stream from webapp (prevents access outside webapp)
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                resp.setContentType(mime);
                // Avoid disclosing server internals; don't send stack traces or file system paths
                // Set safe caching headers (adjust as needed)
                resp.setHeader("Cache-Control", "private, max-age=3600");
                resp.setHeader("X-Content-Type-Options", "nosniff");

                // Stream the image with a fixed-size buffer
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            } catch (IllegalStateException ise) {
                // Typically from response.getOutputStream() called in invalid state
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to write response.");
            } catch (IOException ioe) {
                // Generic IO error; do not expose internal details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error while serving image.");
            } catch (Exception e) {
                // Catch-all to avoid unexpected exceptions leaking details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            }
        }
    }
}
