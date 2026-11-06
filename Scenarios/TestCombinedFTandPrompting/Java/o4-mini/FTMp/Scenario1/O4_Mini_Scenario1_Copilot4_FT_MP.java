import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * ImageServlet serves image files from the "/images" folder in the web application's resources.
 *
 * Security considerations addressed:
 * - Strict filename validation (whitelist of allowed characters and extensions) to prevent path traversal (CWE-22).
 * - Uses ServletContext.getResourceAsStream to avoid direct file system access.
 * - Sets Content-Type from container-recognized MIME types; falls back to binary stream.
 * - Adds security headers: X-Content-Type-Options, Content-Security-Policy.
 * - Streams with a bounded buffer; uses try-with-resources to avoid resource leaks.
 * - Returns appropriate HTTP status codes without leaking internal details.
 */
public class O4_Mini_Scenario1_Copilot4_FT_MP {

    public static class ImageServlet extends HttpServlet {

        // Regex: only letters, digits, dot, underscore, hyphen; extensions jpg|jpeg|png|gif (case insensitive)
        private static final String FILENAME_REGEX = "^[a-zA-Z0-9._-]+\\.(?i)(jpg|jpeg|png|gif)$";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Retrieve and validate the 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                // Missing parameter
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }
            if (!fileName.matches(FILENAME_REGEX)) {
                // Invalid filename: reject to prevent path traversal or unexpected extensions
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 2. Build the resource path under /images
            String resourcePath = "/images/" + fileName;

            ServletContext context = getServletContext();
            // 3. Load the image as a resource stream (read-only, safe)
            try (InputStream imageStream = context.getResourceAsStream(resourcePath)) {
                if (imageStream == null) {
                    // Not found or not a resource within the webapp
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // 4. Determine MIME type; default to binary
                String mimeType = context.getMimeType(fileName);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                // 5. Set response headers with secure defaults
                resp.setContentType(mimeType);
                resp.setHeader("X-Content-Type-Options", "nosniff");  // Prevent MIME sniffing
                // Only allow this resource, no inline JS/CSS/etc.
                resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'");
                // Encourage caching but private to user agent
                resp.setHeader("Cache-Control", "private, max-age=3600, no-transform");
                // Suggest inline rendering; filename quoted to prevent HTTP header injection
                resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");

                // 6. Stream the image bytes to the response output
                try (ServletOutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = imageStream.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            }
        }
    }
}