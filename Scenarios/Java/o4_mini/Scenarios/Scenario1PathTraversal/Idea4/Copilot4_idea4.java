import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Secure ImageServlet serving images from the /images directory.
 * • Whitelists filenames to prevent path traversal.
 * • Uses getResourceAsStream to restrict file access to webapp resources.
 * • Sends minimal error info to client and logs details internally.
 * • Sets security headers to prevent MIME sniffing and control caching.
 */
public class Copilot4_idea4 {
    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        // Only allow filenames like “foo-bar_01.jpg”, case-insensitive for extensions
        private static final Pattern FILENAME_PATTERN =
                Pattern.compile("^[a-zA-Z0-9._-]+\\.(png|jpe?g|gif)$", Pattern.CASE_INSENSITIVE);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Validate input
            String file = req.getParameter("file");
            if (file == null || !FILENAME_PATTERN.matcher(file).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            ServletContext context = getServletContext();
            String resourcePath = "/images/" + file;

            // 2. Load resource securely from webapp
            try (InputStream is = context.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                    return;
                }

                // 3. Determine MIME type
                String mimeType = context.getMimeType(file);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // 4. Security headers
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("Cache-Control", "no-transform, public, max-age=3600");
                resp.setHeader("Content-Disposition", "inline; filename=\"" + file + "\"");

                // 5. Stream the content
                try (OutputStream os = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    os.flush();
                }
            } catch (IOException e) {
                // Log full stack trace internally, send generic error to client
                context.log("Error serving file: " + file, e);
                if (!resp.isCommitted()) {
                    resp.reset();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                }
            }
        }
    }
}
