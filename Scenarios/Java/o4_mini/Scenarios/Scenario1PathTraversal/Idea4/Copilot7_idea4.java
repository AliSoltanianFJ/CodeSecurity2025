import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
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
import java.nio.file.StandardOpenOption;

public class Copilot7_idea4 {

    public static class ImageServlet extends HttpServlet {
        // Base directory where images are stored, resolved at init time
        private Path baseDir;
        // Maximum allowed image size: 10 MB
        private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            ServletContext ctx = config.getServletContext();
            // Allow override via init-param; otherwise serve from webapp /images folder
            String dir = ctx.getInitParameter("imageBase");
            if (dir == null || dir.isBlank()) {
                dir = ctx.getRealPath("/images");
            }
            try {
                Path p = Paths.get(dir).toAbsolutePath().normalize();
                if (!Files.isDirectory(p)) {
                    throw new ServletException("Configured image base is not a directory: " + p);
                }
                this.baseDir = p;
            } catch (Exception e) {
                throw new ServletException("Failed to initialize image base directory", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Validate 'file' parameter presence
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }

            // 2. Strict whitelist for filename: letters, digits, underscore, hyphen, dot
            if (!fileName.matches("[A-Za-z0-9._-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 3. Resolve and normalize path to prevent directory traversal
            Path requestedFile = baseDir.resolve(fileName).normalize();
            if (!requestedFile.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // 4. Check existence and file type
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // 5. Enforce maximum file size
            long size = Files.size(requestedFile);
            if (size > MAX_FILE_SIZE) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "File too large.");
                return;
            }

            // 6. Determine MIME type securely via servletContext
            String contentType = getServletContext().getMimeType(requestedFile.toString());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            resp.setContentLengthLong(size);

            // 7. Stream file contents using try-with-resources
            try (InputStream in = Files.newInputStream(requestedFile, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                // Log the exception without leaking internal paths to the client
                log("Error serving image: " + requestedFile, e);
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error while reading file.");
                }
            }
        }
    }
}
