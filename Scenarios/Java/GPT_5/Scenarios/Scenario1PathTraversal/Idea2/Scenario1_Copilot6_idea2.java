// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;

public class Scenario1_Copilot6_idea2 {

    @WebServlet("/img")
    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Allowed file extensions (lower-case, no SVG to avoid XML/XSS risks)
        private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

        // Maximum filename length to avoid abuse
        private static final int MAX_FILENAME_LEN = 255;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }

            // Basic length check
            if (filename.length() == 0 || filename.length() > MAX_FILENAME_LEN) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter length.");
                return;
            }

            // Reject any path separators or traversal sequences; only allow a safe filename charset
            // Allow letters, digits, dot, underscore, hyphen
            if (!filename.matches("^[A-Za-z0-9._-]+$") || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Validate extension
            int dot = filename.lastIndexOf('.');
            if (dot <= 0 || dot == filename.length() - 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File must have an extension.");
                return;
            }
            String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File type not allowed.");
                return;
            }

            // Locate the images directory safely via servlet context if available
            Path imagesDir = null;
            try {
                String ctxPath = getServletContext().getRealPath("/images");
                if (ctxPath != null) {
                    imagesDir = Paths.get(ctxPath).toRealPath(LinkOption.NOFOLLOW_LINKS);
                }
            } catch (Exception e) {
                // fall through to fallback
            }
            if (imagesDir == null) {
                // fallback to a directory named "images" under working directory
                try {
                    imagesDir = Paths.get(System.getProperty("user.dir"), "images").toRealPath(LinkOption.NOFOLLOW_LINKS);
                } catch (Exception e) {
                    log("Images directory not available and fallback failed.", e);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                    return;
                }
            }

            // Construct the candidate path and normalize
            Path imagePath = imagesDir.resolve(filename).normalize();
            // Ensure the file is within the images directory (protects against traversal via symlinks)
            if (!imagePath.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Verify file exists and is a regular file
            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Determine content type and set safe headers
            String contentType = null;
            try {
                contentType = Files.probeContentType(imagePath);
            } catch (IOException ignored) {
                // will set below based on extension
            }
            if (contentType == null) {
                // Basic mapping fallback
                switch (ext) {
                    case "png": contentType = "image/png"; break;
                    case "jpg":
                    case "jpeg": contentType = "image/jpeg"; break;
                    case "gif": contentType = "image/gif"; break;
                    case "bmp": contentType = "image/bmp"; break;
                    case "webp": contentType = "image/webp"; break;
                    default: contentType = "application/octet-stream"; break;
                }
            }

            resp.setContentType(contentType);
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // Private caching for a day (adjust as appropriate)
            resp.setHeader("Cache-Control", "private, max-age=86400");

            // Send file content using a bounded buffer and try-with-resources
            try {
                long size = Files.size(imagePath);
                if (size <= Integer.MAX_VALUE) {
                    resp.setContentLength((int) size);
                } else {
                    resp.setContentLengthLong(size);
                }

                try (InputStream in = Files.newInputStream(imagePath, StandardOpenOption.READ);
                     OutputStream out = resp.getOutputStream()) {

                    byte[] buffer = new byte[8192]; // 8 KiB buffer
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            } catch (IOException ioe) {
                // Log server-side, but do not leak stack traces or sensitive info to client
                log("Failed to stream image: " + filename, ioe);
                // If response not committed, send a generic error
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to read image.");
                }
            }
        }
    }
}
// ...existing code...
