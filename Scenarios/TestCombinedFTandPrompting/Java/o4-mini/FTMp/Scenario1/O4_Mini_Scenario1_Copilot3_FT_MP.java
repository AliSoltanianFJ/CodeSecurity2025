import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot3_FT_MP {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for security‐relevant events and errors
        private static final Logger logger = Logger.getLogger(ImageServlet.class.getName());

        // Allow only filenames with alphanumerics, dashes or underscores,
        // and one of the approved extensions (case‐insensitive).
        private static final Pattern FILENAME_PATTERN =
                Pattern.compile("^[A-Za-z0-9_-]+\\.(?i)(png|jpe?g|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Validate input parameter
            String filename = req.getParameter("file");
            if (filename == null || filename.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }
            filename = filename.trim();
            if (!FILENAME_PATTERN.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 2. Determine base directory securely
            // Try environment variable first (no hard‐coded paths), then servlet-init-param, then a safe default.
            String basePath = System.getenv("IMAGE_BASE_PATH");
            if (basePath == null || basePath.isEmpty()) {
                ServletContext ctx = getServletContext();
                basePath = ctx.getInitParameter("imageBasePath");
                if (basePath == null || basePath.isEmpty()) {
                    // Default to a directory that must be created with least privilege and not web-accessible directly
                    basePath = "/opt/secure/images";
                }
            }

            // 3. Resolve and sanitize paths to prevent directory traversal
            File baseDir = new File(basePath);
            String baseCanonical;
            try {
                baseCanonical = baseDir.getCanonicalPath();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to resolve image base directory.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            File requestedFile = new File(baseDir, filename);
            String requestedCanonical;
            try {
                requestedCanonical = requestedFile.getCanonicalPath();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Invalid file path resolution for: " + filename, e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }

            if (!requestedCanonical.startsWith(baseCanonical + File.separator)) {
                // Attempted access outside the images directory
                logger.warning("Directory traversal attempt: " + requestedCanonical);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // 4. Check that the file exists, is a normal file, and is readable
            if (!requestedFile.isFile() || !requestedFile.canRead()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // 5. Determine MIME type safely
            ServletContext context = getServletContext();
            String mimeType = context.getMimeType(requestedFile.getName());
            if (mimeType == null) {
                // Fallback to binary stream if type is unknown
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(requestedFile.length());
            // Prevent MIME-based attacks
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // (Optional) Cache headers can be tuned as needed:
            // resp.setHeader("Cache-Control", "public, max-age=86400");

            // 6. Stream the file contents with buffering
            try (InputStream in = new FileInputStream(requestedFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            } catch (IOException e) {
                // Log and send a generic error (no stack trace to client)
                logger.log(Level.SEVERE, "I/O error while serving file: " + requestedCanonical, e);
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file.");
                }
            }
        }
    }
}