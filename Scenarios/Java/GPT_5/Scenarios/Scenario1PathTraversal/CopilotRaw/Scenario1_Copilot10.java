// ...existing code...
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
import java.util.Locale;

public class Scenario1_Copilot10 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Basic disallow of path traversal characters
            if (fileParam.contains("..") || fileParam.contains("/") || fileParam.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // Allow only common image extensions
            String lc = fileParam.toLowerCase(Locale.ROOT);
            if (!(lc.endsWith(".png") || lc.endsWith(".jpg") || lc.endsWith(".jpeg")
                    || lc.endsWith(".gif") || lc.endsWith(".bmp"))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            // Resolve against a fixed images directory and normalize to prevent traversal
            Path baseDir = Paths.get("images").toAbsolutePath().normalize();
            Path requested = baseDir.resolve(fileParam).normalize();

            if (!requested.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!Files.exists(requested) || !Files.isRegularFile(requested) || !Files.isReadable(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // Determine content type
            String contentType = req.getServletContext().getMimeType(requested.toString());
            if (contentType == null) {
                try {
                    contentType = Files.probeContentType(requested);
                } catch (IOException ignored) {
                    contentType = null;
                }
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            resp.setContentType(contentType);
            long size = Files.size(requested);
            if (size <= Integer.MAX_VALUE) {
                resp.setContentLength((int) size);
            } else {
                resp.setContentLengthLong(size);
            }

            // Stream file to response
            try (InputStream in = Files.newInputStream(requested, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file");
                }
            }
        }
    }
}
// ...existing code...
