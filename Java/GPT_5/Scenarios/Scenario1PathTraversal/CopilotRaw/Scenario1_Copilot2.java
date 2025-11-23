import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Scenario1_Copilot2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            ServletContext ctx = getServletContext();
            String imagesRealPath = ctx.getRealPath("/images");
            if (imagesRealPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not available");
                return;
            }

            File baseDir = new File(imagesRealPath);
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found");
                return;
            }

            // Construct requested file and prevent path traversal by canonical path check
            File requested = new File(baseDir, fileParam);
            String baseCanonical;
            String requestedCanonical;
            try {
                baseCanonical = baseDir.getCanonicalPath();
                requestedCanonical = requested.getCanonicalPath();
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to resolve file path");
                return;
            }

            // Ensure requested file is inside the images directory
            if (!requestedCanonical.equals(baseCanonical) && !requestedCanonical.startsWith(baseCanonical + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!requested.exists() || !requested.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // Determine mime type
            String mime = ctx.getMimeType(requested.getName());
            if (mime == null) {
                try {
                    mime = Files.probeContentType(requested.toPath());
                } catch (IOException ignored) {
                    // fallback below
                }
            }
            if (mime == null) {
                mime = "application/octet-stream";
            }

            resp.setContentType(mime);
            long length = requested.length();
            if (length <= Integer.MAX_VALUE) {
                resp.setContentLength((int) length);
            } else {
                resp.setContentLengthLong(length);
            }

            // Stream file to response
            try (InputStream in = new BufferedInputStream(new FileInputStream(requested));
                 ServletOutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            } catch (IOException e) {
                // If an error occurs while streaming, attempt to send an error if not committed
                if (!resp.isCommitted()) {
                    resp.reset();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                }
            }
        }
    }
}
