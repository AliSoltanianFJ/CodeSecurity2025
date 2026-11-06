import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ImageServlet securely serves image files from the "/images" folder in the webapp.
 *
 * Security decisions and mitigations:
 * - Input validation: only allows filenames matching a strict regex; prohibits path traversal.
 * - Content-type is determined by file extension; rejects unknown/unsupported types.
 * - Uses ServletContext.getResourceAsStream, so files outside the webapp cannot be accessed.
 * - Sets "X-Content-Type-Options: nosniff" to prevent MIME-type sniffing.
 * - Handles errors without leaking stack traces or internal paths.
 * - Streams data in a buffered manner to avoid loading entire file in memory.
 * - No secrets or configuration data are hardcoded here.
 */
public class O4_Mini_Scenario1_Copilot1_FT_MP {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Strict filename pattern: alphanumeric, underscores, hyphens, followed by a permitted extension.
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_\\-]+\\.(?i)(jpg|jpeg|png|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Retrieve and validate the "file" parameter
            String filename = req.getParameter("file");
            if (filename == null || filename.isEmpty()) {
                // Missing parameter
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }
            if (!FILENAME_PATTERN.matcher(filename).matches()) {
                // Invalid filename: protects against path traversal, invalid chars, and disallowed extensions
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // 2. Determine content type by extension
            String lower = filename.toLowerCase(Locale.ROOT);
            String contentType;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lower.endsWith(".png")) {
                contentType = "image/png";
            } else if (lower.endsWith(".gif")) {
                contentType = "image/gif";
            } else {
                // This should never happen because of the regex, but we check defensively
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported image type");
                return;
            }

            // 3. Set secure response headers
            resp.setContentType(contentType);
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // Optionally, add caching headers if desired:
            // resp.setHeader("Cache-Control", "public, max-age=3600");

            // 4. Stream the resource from the webapp's /images directory
            // Using ServletContext.getResourceAsStream ensures no file-system escape
            String resourcePath = "/images/" + filename;
            try (InputStream in = getServletContext().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // File not found
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // Write the image to the response output stream
                ServletOutputStream out = resp.getOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            } catch (IOException ioe) {
                // Log the exception on the server; do not expose details to the client
                log("Error serving image: " + filename, ioe);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve image");
            }
        }
    }
}
