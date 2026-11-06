import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot10_FT_MP {

    public static class ImageServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(ImageServlet.class.getName());

        // Only allow simple filenames: alphanumeric, dots, dashes, underscores
        // Prevents directory traversal and other tricks.
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

        // Supported image extensions (lowercase)
        private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"};

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Retrieve and validate the 'file' parameter
            String fileParam = req.getParameter("file");
            if (fileParam == null || !FILENAME_PATTERN.matcher(fileParam).matches() || !hasAllowedExtension(fileParam)) {
                // Bad request if parameter is missing, too long, contains illegal chars, or wrong extension
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image request");
                return;
            }

            // 2. Normalize the requested path to a known directory under the webapp
            // Using ServletContext.getResourceAsStream guarantees we stay in the WAR
            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + fileParam;

            // 3. Attempt to load the resource as a stream
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // Not found
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // 4. Determine and set the content type
                String mimeType = ctx.getMimeType(fileParam);
                if (mimeType == null) {
                    // Fallback if container doesn't know this extension
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // 5. Set caching headers (adjust max-age as appropriate)
                // Clients and proxies may cache images for 1 hour
                resp.setHeader("Cache-Control", "public, max-age=3600");

                // 6. Stream the image data to the client safely
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            } catch (IOException ioe) {
                // 7. On any I/O error, log at a level that your operations team monitors
                LOGGER.log(Level.WARNING, "Error serving image: " + resourcePath, ioe);

                // 8. Return a generic 500 error without stack trace details
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve image");
                }
            }
        }

        /**
         * Ensures the filename ends with one of the allowed extensions (case‐insensitive).
         */
        private static boolean hasAllowedExtension(String filename) {
            String lower = filename.toLowerCase();
            for (String ext : ALLOWED_EXTENSIONS) {
                if (lower.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    }
}