import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot10_FT_TOSP {

    public static class ImageServlet extends HttpServlet {
        // Only allow filenames like "foo.png", "bar-123.JPG", etc.
        private static final Pattern FILENAME_PATTERN = 
            Pattern.compile("^[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|gif|bmp)$", Pattern.CASE_INSENSITIVE);
        private static final int BUFFER_SIZE = 8 * 1024;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }
            fileName = fileName.trim();
            if (!FILENAME_PATTERN.matcher(fileName).matches()) {
                // Invalid filename or prohibited characters / extension
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Build the resource path under /images in the WAR
            String resourcePath = "/images/" + fileName;
            ServletContext context = getServletContext();

            // Try to load the image as a servlet resource. This blocks any
            // path‐traversal (no direct file system access).
            try (InputStream in = context.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // Not found or not readable
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // Determine content type (may return null)
                String mimeType = context.getMimeType(resourcePath);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // Stream the file in a buffered manner
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            } catch (IOException ioe) {
                // Log internally; do NOT expose stack trace to client
                context.log("Error streaming image: " + resourcePath, ioe);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to retrieve image.");
            }
        }
    }
}

