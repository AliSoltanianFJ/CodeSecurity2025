import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Copilot9_idea4 {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Base path inside the WAR; avoid hard-coding filesystem paths
        private static final String IMAGE_BASE_PATH = "/images/";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Enforce HTTPS: reject non-secure requests
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required");
                return;
            }

            // 2. Fetch and validate the 'file' parameter to prevent path traversal (CWE-22)
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }
            // Allow only letters, digits, underscore, hyphen, dot; require a safe extension
            if (!fileName.matches("^[A-Za-z0-9_\\-]+\\.(?i)(jpg|jpeg|png|gif|bmp)$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = IMAGE_BASE_PATH + fileName;

            // 3. Load resource via ServletContext to avoid direct filesystem access
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // 4. Determine MIME type safely
                String mimeType = ctx.getMimeType(fileName);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // 5. Set cache headers (optional; adjust per policy)
                resp.setHeader("Cache-Control", "public, max-age=86400");

                // 6. Stream the image with a fixed-size buffer
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            } catch (IOException e) {
                // 7. Log the exception internally; do not leak stack traces or internal paths
                log("Error serving image: " + fileName, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
