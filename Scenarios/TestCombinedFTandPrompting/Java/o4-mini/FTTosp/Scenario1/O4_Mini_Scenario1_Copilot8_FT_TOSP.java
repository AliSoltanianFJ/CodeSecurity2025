import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class O4_Mini_Scenario1_Copilot8_FT_TOSP {

    public static class ImageServlet extends HttpServlet {
        private static final int BUFFER_SIZE = 8192;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Get and validate the filename parameter
            String filename = req.getParameter("filename");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing filename parameter");
                return;
            }

            // Only allow alphanumerics, underscore, hyphen and dot
            if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Only allow common image extensions (case‐insensitive)
            String lower = filename.toLowerCase(Locale.ENGLISH);
            if (!(lower.endsWith(".png")
                    || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif"))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            // 2) Resolve the /images directory in the webapp
            ServletContext context = getServletContext();
            String imagesPath = context.getRealPath("/images");
            if (imagesPath == null) {
                // Could not map /images to a filesystem path
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory unavailable");
                return;
            }
            File imagesDir = new File(imagesPath);

            // 3) Build the requested file path, then canonicalize both
            File imageFile = new File(imagesDir, filename);
            String dirCanonical;
            String fileCanonical;
            try {
                dirCanonical  = imagesDir.getCanonicalPath();
                fileCanonical = imageFile.getCanonicalPath();
            } catch (IOException e) {
                context.log("Failed to canonicalize file paths", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 4) Prevent path traversal: requested file must live under imagesDir
            if (!fileCanonical.startsWith(dirCanonical + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // 5) Check existence & readability
            if (!imageFile.isFile() || !imageFile.canRead()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // 6) Determine and set MIME type
            String mimeType = context.getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(imageFile.length());

            // 7) Stream the file in a fixed-size buffer
            try (InputStream in = new FileInputStream(imageFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Log internal error; do not expose details to client
                context.log("Error streaming image file", e);
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to send image");
                }
            }
        }
    }
}

