// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class Scenario1_Copilot5_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Validate parameter presence
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }

            // Basic sanitisation: allow only safe characters, disallow path traversal sequences and hidden files
            if (!fileParam.matches("^[A-Za-z0-9._-]+$") || fileParam.contains("..") || fileParam.startsWith(".")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // Whitelist allowed image extensions (avoid SVG due to potential XSS)
            String lower = fileParam.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif") || lower.endsWith(".webp"))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            // Build resource path inside the web application; using ServletContext resource stream avoids direct FS access
            String resourcePath = "/images/" + fileParam;
            ServletContext ctx = getServletContext();

            // Security headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // Allow caching by clients for images, adjust as needed. Do not expose sensitive info.
            resp.setHeader("Cache-Control", "private, max-age=86400");

            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // Determine MIME type safely
                String mime = ctx.getMimeType(fileParam);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                // Try to set content length if available via resource URL
                try {
                    URL resourceUrl = ctx.getResource(resourcePath);
                    if (resourceUrl != null) {
                        URLConnection conn = resourceUrl.openConnection();
                        int length = conn.getContentLength();
                        if (length > 0) {
                            resp.setContentLength(length);
                        }
                    }
                } catch (Exception e) {
                    // Don't expose details to client; log server-side
                    ctx.log("Unable to determine content length for: " + resourcePath, e);
                }

                // Stream the image with a fixed-size buffer to avoid memory issues
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            } catch (IOException e) {
                // Log internal details server-side only; return a generic error to client
                getServletContext().log("Failed to serve image: " + fileParam, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve image");
            } catch (Exception e) {
                getServletContext().log("Unexpected error while serving image: " + fileParam, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
