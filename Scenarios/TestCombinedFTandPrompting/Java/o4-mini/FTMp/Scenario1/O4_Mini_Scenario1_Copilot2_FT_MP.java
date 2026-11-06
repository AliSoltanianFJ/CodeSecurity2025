import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Scenario1.ImageServlet
 *
 * Securely serves image files located under WEB-INF/images/.
 * 
 * Security measures:
 *  - Whitelist‐only filenames via regex (prevents path‐traversal, control characters, etc.).
 *  - Files must end in .png/.jpg/.jpeg/.gif (case‐insensitive).
 *  - Loads images via ServletContext.getResourceAsStream() under WEB-INF (not directly web‐accessible).
 *  - Sets explicit Content-Type via servlet’s MIME map; falls back to application/octet-stream.
 *  - Disables MIME sniffing (X-Content-Type-Options: nosniff).
 *  - Applies safe caching headers.
 *  - Catches IO errors, logs only minimal info server‐side, and returns generic 500 to clients.
 *  - No stack traces or internal paths are ever exposed to the caller.
 */
public class O4_Mini_Scenario1_Copilot2_FT_MP {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Only allow filenames matching this pattern:
        //   [letters, digits, dot, underscore, hyphen]+
        //   ending with .png, .jpg, .jpeg or .gif (case‐insensitive)
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]+\\.(?i)(png|jpe?g|gif)$");

        // Base path under WEB-INF where images reside
        private static final String IMAGE_BASE_PATH = "/WEB-INF/images/";

        // 8KB buffer for streaming
        private static final int BUFFER_SIZE = 8 * 1024;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Validate presence of the 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter 'file'.");
                return;
            }

            // 2) Validate against whitelist regex to prevent path‐traversal, control chars, etc.
            if (!FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
                return;
            }

            // 3) Build the internal resource path under WEB-INF
            String resourcePath = IMAGE_BASE_PATH + fileName;

            // 4) Attempt to load the image as an internal resource
            try (InputStream in = getServletContext().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // File not found or not under allowed folder
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // 5) Determine MIME type from servlet context; fallback to binary stream
                String mimeType = getServletContext().getMimeType(fileName);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // 6) Prevent MIME sniffing by browsers
                resp.setHeader("X-Content-Type-Options", "nosniff");

                // 7) Safe caching headers (adjust max-age as your policy dictates)
                resp.setHeader("Cache-Control", "public, max-age=86400");

                // 8) Stream the image data to the client
                ServletOutputStream out = resp.getOutputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            } catch (IOException ioe) {
                // 9) Log minimal information server‐side; don’t expose stack trace or paths
                getServletContext().log("Error serving image '" + fileName + "': " + ioe.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
