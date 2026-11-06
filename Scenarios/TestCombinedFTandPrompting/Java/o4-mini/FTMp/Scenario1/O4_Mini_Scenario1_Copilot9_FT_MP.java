import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot9_FT_MP {

    public static class ImageServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(ImageServlet.class.getName());

        // ONLY allow filenames with letters, digits, underscores or hyphens,
        // plus a whitelisted extension (case-insensitive).
        private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_-]+\\.(?i)(png|jpe?g|gif)$"
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || !FILENAME_PATTERN.matcher(fileName).matches()) {
                // Invalid or potentially dangerous file name
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image request");
                return;
            }

            // Build a path under WEB-INF to prevent direct external access
            String imagePath = "/WEB-INF/images/" + fileName;
            ServletContext ctx = getServletContext();

            // Attempt to locate the resource using the container's class-path
            URL resourceUrl = ctx.getResource(imagePath);
            if (resourceUrl == null) {
                // File does not exist
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Determine MIME type
            String mimeType = ctx.getMimeType(fileName);
            if (mimeType == null) {
                // Fallback to a safe default
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            // Encourage caching but only by the browser (private), for 1 hour
            resp.setHeader("Cache-Control", "private, max-age=3600");

            try (
                // Open a connection to get content length
                InputStream in = resourceUrl.openStream();
                OutputStream out = resp.getOutputStream()
            ) {
                // Try to set content length if available
                try {
                    URLConnection conn = resourceUrl.openConnection();
                    long length = conn.getContentLengthLong();
                    if (length > 0) {
                        resp.setContentLengthLong(length);
                    }
                } catch (Exception e) {
                    // Non-fatal if we can't determine length
                    LOGGER.log(Level.FINE, "Could not determine resource length", e);
                }

                // Stream the data in 8KB chunks
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            } catch (IOException ioe) {
                // Log internally, but only return a generic error to client
                LOGGER.log(Level.WARNING, "Error streaming image " + fileName, ioe);
                if (!resp.isCommitted()) {
                    resp.reset();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                   "Unable to process image request");
                }
            }
        }
    }
}