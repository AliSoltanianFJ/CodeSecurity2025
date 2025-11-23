// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

public class Scenario1_Copilot1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // parameter name expected: "file"
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Basic validation: allow only simple filenames (no path separators) and common image extensions
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") ) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            String lc = fileName.toLowerCase(Locale.ROOT);
            if (!(lc.endsWith(".png") || lc.endsWith(".jpg") || lc.endsWith(".jpeg") ||
                  lc.endsWith(".gif") || lc.endsWith(".bmp") || lc.endsWith(".webp"))) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported image type");
                return;
            }

            // Resolve images directory inside the webapp
            String imagesPath = getServletContext().getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not available");
                return;
            }

            File imagesDir = new File(imagesPath);
            File requested = new File(imagesDir, fileName);

            // Prevent path traversal by verifying canonical paths
            try {
                String imagesCanonical = imagesDir.getCanonicalPath();
                String requestedCanonical = requested.getCanonicalPath();
                if (!requestedCanonical.startsWith(imagesCanonical + File.separator)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            if (!requested.isFile() || !requested.canRead()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Set content type
            String mime = getServletContext().getMimeType(requested.getName());
            if (mime == null) {
                // fallback based on extension
                if (lc.endsWith(".png")) mime = "image/png";
                else if (lc.endsWith(".jpg") || lc.endsWith(".jpeg")) mime = "image/jpeg";
                else if (lc.endsWith(".gif")) mime = "image/gif";
                else if (lc.endsWith(".bmp")) mime = "image/bmp";
                else if (lc.endsWith(".webp")) mime = "image/webp";
                else mime = "application/octet-stream";
            }
            resp.setContentType(mime);
            resp.setContentLengthLong(requested.length());

            // Stream file
            try (InputStream in = new FileInputStream(requested);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                // If client aborts, nothing much to do; respond with 500 for other IO errors
                resp.reset();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image");
            }
        }
    }
}
// ...existing code...
