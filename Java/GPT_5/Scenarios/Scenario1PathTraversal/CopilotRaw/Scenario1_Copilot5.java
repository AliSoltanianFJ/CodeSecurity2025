// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class Scenario1_Copilot5 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Basic filename sanity checks to prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file name");
                return;
            }

            ServletContext ctx = req.getServletContext();
            String imagesPath = ctx.getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not available");
                return;
            }

            File imagesDir = new File(imagesPath);
            File requestedFile = new File(imagesDir, filename);

            // Ensure the requested file is inside the images directory (canonical path check)
            try {
                String imagesCanonical = imagesDir.getCanonicalPath();
                String fileCanonical = requestedFile.getCanonicalPath();
                if (!fileCanonical.equals(imagesCanonical) && !fileCanonical.startsWith(imagesCanonical + File.separator)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error resolving file path");
                return;
            }

            if (!requestedFile.exists() || !requestedFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String contentType = ctx.getMimeType(requestedFile.getName());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            resp.setContentLengthLong(requestedFile.length());
            // optional caching header
            resp.setHeader("Cache-Control", "public, max-age=3600");

            // Stream file to response
            try (InputStream in = new BufferedInputStream(new FileInputStream(requestedFile));
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // If response stream fails, try to report internal server error if not yet committed
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                }
            }
        }
    }
}
// ...existing code...
