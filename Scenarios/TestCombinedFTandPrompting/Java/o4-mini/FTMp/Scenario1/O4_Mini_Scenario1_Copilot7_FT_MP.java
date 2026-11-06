import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot7_FT_MP {

    public static class ImageServlet extends HttpServlet {
        // Allow only filenames with letters, digits, dot, underscore or hyphen
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
        private static final int BUFFER_SIZE = 8192;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Retrieve and validate the 'file' parameter
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }
            if (!FILENAME_PATTERN.matcher(fileParam).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 2. Resolve the images directory securely via the servlet context
            String imagesDirPath = getServletContext().getRealPath("/images");
            if (imagesDirPath == null) {
                // real path could be null if the container cannot translate the virtual path
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not configured.");
                return;
            }
            File imagesDir = new File(imagesDirPath);
            if (!imagesDir.isDirectory() || !imagesDir.canRead()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not accessible.");
                return;
            }

            // 3. Construct the requested file and prevent directory traversal
            File requestedFile = new File(imagesDir, fileParam);
            String imagesCanonical = imagesDir.getCanonicalPath();
            String fileCanonical = requestedFile.getCanonicalPath();
            // Ensure the requested file is inside the images directory
            if (!fileCanonical.startsWith(imagesCanonical + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // 4. Verify the target is a file and is readable
            if (!requestedFile.isFile() || !requestedFile.canRead()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // 5. Determine and validate the MIME type
            String mimeType = getServletContext().getMimeType(requestedFile.getName());
            if (mimeType == null) {
                // Fallback to probing via java.nio
                mimeType = Files.probeContentType(requestedFile.toPath());
            }
            // Only serve recognized image types
            if (mimeType == null || !mimeType.startsWith("image/")) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Not an image file.");
                return;
            }

            // 6. Set secure response headers
            resp.setContentType(mimeType);
            resp.setHeader("Content-Disposition", "inline; filename=\"" + requestedFile.getName() + "\"");
            // Prevent MIME-sniffing in browsers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // (Optional) Caching policy; adjust as needed
            resp.setHeader("Cache-Control", "public, max-age=3600");
            resp.setContentLengthLong(requestedFile.length());

            // 7. Stream the file to the client using a safe buffer size
            try (FileInputStream in = new FileInputStream(requestedFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Log internally without exposing details to the client
                log("Error serving image '" + fileParam + "': " + e.getMessage());
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file.");
                }
            }
        }
    }
}
