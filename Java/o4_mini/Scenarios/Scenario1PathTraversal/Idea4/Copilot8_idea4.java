import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Copilot8_idea4 {

    public static class ImageServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(ImageServlet.class.getName());
        private Path baseDir;

        @Override
        public void init() throws ServletException {
            String baseDirParam = getServletConfig().getInitParameter("imagesPath");
            if (baseDirParam == null || baseDirParam.trim().isEmpty()) {
                throw new ServletException("Initialization parameter 'imagesPath' is required.");
            }
            baseDir = Paths.get(baseDirParam).toAbsolutePath().normalize();
            if (!Files.isDirectory(baseDir) || !Files.isReadable(baseDir)) {
                throw new ServletException("Configured imagesPath is not a readable directory: " + baseDir);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Require HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
                return;
            }

            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }

            // Only allow simple filenames (alphanum, underscore, hyphen, dot)
            if (!fileName.matches("[A-Za-z0-9_\\-\\.]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Resolve and normalize against baseDir
            Path imagePath = baseDir.resolve(fileName).normalize();
            if (!imagePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized access.");
                return;
            }

            if (!Files.isRegularFile(imagePath) || !Files.isReadable(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            String contentType = Files.probeContentType(imagePath);
            if (contentType == null || !contentType.startsWith("image/")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Unsupported file type.");
                return;
            }

            // Security headers
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            resp.setHeader("Content-Security-Policy", "default-src 'none';");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

            resp.setContentType(contentType);
            resp.setContentLengthLong(Files.size(imagePath));

            try (InputStream in = Files.newInputStream(imagePath);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error streaming image: " + imagePath, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
