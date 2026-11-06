import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.*;

/**
 * A servlet that safely serves image files from /WEB-INF/images.
 * 
 * Security measures applied:
 *  - Enforces HTTPS (req.isSecure()) to ensure data-in-transit is encrypted.
 *  - Validates filename against a strict whitelist regex (no path separators, only alphanumerics, dots, dashes, underscores).
 *  - Resolves and normalizes path via java.nio.file to prevent directory traversal.
 *  - Restricts to a fixed base directory under /WEB-INF/images (not web-accessible).
 *  - Checks for existence/readability, enforces a max file size limit (e.g. 10 MB).
 *  - Uses the servlet container’s MIME type map plus URLConnection fallback, and ensures it's an image/*
 *  - Sets safe caching headers.
 *  - Streams bytes via try-with-resources to avoid resource leaks.
 *  - Logs errors internally without exposing stack traces to clients.
 */
public class O4_Mini_Scenario1_Copilot5_FT_MP {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Allow only filenames like "foo.png", "bar-01.jpg", "baz_02.gif"
        private static final String FILENAME_REGEX = "^[A-Za-z0-9._-]+\\.(?i)(png|jpe?g|gif|bmp|webp)$";
        private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Enforce HTTPS
            if (!req.isSecure()) {
                // Note: in some deployments SSL is terminated at a load-balancer; ensure X-Forwarded-Proto is handled if needed.
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
                return;
            }

            // 2) Validate "file" parameter
            String fileName = req.getParameter("file");
            if (fileName == null || !fileName.matches(FILENAME_REGEX)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image parameter");
                return;
            }

            // 3) Locate base images directory under WEB-INF (not directly web-accessible)
            ServletContext ctx = getServletContext();
            String imagesPath = ctx.getRealPath("/WEB-INF/images");
            if (imagesPath == null) {
                // Cannot determine real path => configuration error
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                return;
            }

            Path baseDir;
            try {
                // Resolve symlinks; do NOT follow symlinks later during normalization
                baseDir = Paths.get(imagesPath)
                               .toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                ctx.log("Cannot resolve images directory: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 4) Resolve requested file and normalize
            Path requested = baseDir.resolve(fileName).normalize();

            // 5) Prevent directory traversal: requested must start with baseDir
            if (!requested.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            File imageFile = requested.toFile();

            // 6) Existence, readability, not a directory
            if (!imageFile.exists() || !imageFile.isFile() || !imageFile.canRead()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // 7) Enforce max file size
            long fileSize = imageFile.length();
            if (fileSize > MAX_FILE_SIZE) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Image too large");
                return;
            }

            // 8) Determine MIME type and ensure it's an image
            String mimeType = ctx.getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = URLConnection.guessContentTypeFromName(imageFile.getName());
            }
            if (mimeType == null || !mimeType.startsWith("image/")) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
                return;
            }

            // 9) Set response headers
            resp.setContentType(mimeType);
            resp.setContentLengthLong(fileSize);
            // Cache for 24 hours; adjust per your app’s caching policy
            resp.setHeader("Cache-Control", "private, max-age=86400");

            // 10) Stream the file contents
            try (InputStream in = new FileInputStream(imageFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException ioe) {
                // Log and terminate gracefully; client may have aborted connection
                ctx.log("Error streaming image '" + fileName + "': " + ioe.getMessage());
                // Do not reveal internal details to the client
            }
        }
    }
}