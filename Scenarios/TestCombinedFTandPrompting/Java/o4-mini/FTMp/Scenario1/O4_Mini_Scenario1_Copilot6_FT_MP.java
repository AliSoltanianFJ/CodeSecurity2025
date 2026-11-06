import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Scenario1.ImageServlet
 *
 * Securely serves image files from a server‐side directory.
 * 
 * Security‐relevant decisions:
 * - Whitelisted filename characters and extensions to prevent path traversal and arbitrary file read (CWE-22).
 * - Normalized and canonicalized paths; enforced that the resolved path starts with the images base directory.
 * - No hardcoded secrets; the base directory may be set via IMAGE_BASE_DIR env var or falls back to webapp "/images" folder.
 * - Streams file contents with a buffer to avoid large in-memory footprints.
 * - Uses the servlet container’s logging; does not leak stack traces or file system info to the client.
 * - Sets proper Content-Type and inline Content-Disposition for context‐aware output.
 * - Applies Cache-Control header with secure defaults.
 * - Graceful error handling with appropriate HTTP status codes.
 */
public class O4_Mini_Scenario1_Copilot6_FT_MP {

    public static class ImageServlet extends HttpServlet {

        // Maximum allowed filename length to mitigate DOS via enormous parameter
        private static final int MAX_FILENAME_LENGTH = 100;

        // Whitelisted filename pattern: alphanumeric, underscore, hyphen, ending in .png/.jpg/.jpeg/.gif
        private static final String FILENAME_PATTERN = "^[a-zA-Z0-9_\\-]+\\.(?i)(png|jpe?g|gif)$";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Retrieve and validate the 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty() || fileName.length() > MAX_FILENAME_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing file parameter");
                return;
            }
            if (!fileName.matches(FILENAME_PATTERN)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name or extension");
                return;
            }

            // 2. Determine base directory for images
            //    First try an environment variable (configured securely in container),
            //    otherwise fall back to the webapp's /images folder.
            String baseDir = System.getenv("IMAGE_BASE_DIR");
            if (baseDir == null || baseDir.trim().isEmpty()) {
                // Note: getRealPath may return null if deployed as a packed WAR.
                String realPath = getServletContext().getRealPath("/images");
                if (realPath == null) {
                    getServletContext().log("ImageServlet: unable to resolve images directory");
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                    return;
                }
                baseDir = realPath;
            }

            // 3. Construct and normalize the path, forbid symlink tricks
            Path basePath;
            Path imagePath;
            try {
                basePath = Paths.get(baseDir)
                        .toRealPath(LinkOption.NOFOLLOW_LINKS); // canonicalize
                imagePath = basePath
                        .resolve(fileName)
                        .normalize(); // remove any ../ sequences
            } catch (IOException ioe) {
                // Could not resolve paths safely
                getServletContext().log("ImageServlet: path resolution error for file: " + fileName, ioe);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            // 4. Prevent path traversal: ensure the resolved path is within the base directory
            if (!imagePath.startsWith(basePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // 5. Verify the file exists, is a regular file, and is readable
            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath) || !Files.isReadable(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // 6. Determine MIME type securely
            String mimeType = getServletContext().getMimeType(imagePath.toString());
            if (mimeType == null) {
                // Fallback to a safe default
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // 7. Recommend inline display with a safe Content-Disposition header
            resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");

            // 8. Set caching headers (adjust as your security policy requires)
            resp.setHeader("Cache-Control", "private, max-age=86400"); // 1 day

            // 9. Stream the file contents to the response output stream
            try (InputStream in = Files.newInputStream(imagePath, StandardOpenOption.READ);
                 ServletOutputStream out = resp.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();

            } catch (IOException streamEx) {
                // Log internal details; do not expose stack traces or paths to the client
                getServletContext().log("ImageServlet: error streaming file: " + fileName, streamEx);
                if (!resp.isCommitted()) {
                    resp.reset();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                }
            }
        }
    }
}