import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_4_idea4 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("filename");
            
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required.");
                return;
            }

            // Define the base directory for images (make sure it's write-protected and doesn't allow traversal)
            String baseDir = getServletContext().getRealPath("/images/");
            
            File imageDir = new File(baseDir);
            if (!imageDir.isDirectory()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
                return;
            }

            // Validate the filename to prevent path traversal and invalid characters
            Path resolvedPath = Paths.get(fileName).normalize();
            
            // Build the full file path
            File imagePath = imageDir.toPath().resolve(resolvedPath).toFile();

            // Check if the resolved path is within the base directory
            if (!imagePath.getAbsolutePath().startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid filename or path traversal attempt.");
                return;
            }

            // Verify the file exists and is a regular file
            if (!imagePath.exists() || !imagePath.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Check MIME type to ensure it's an image
            String mimeType = Files.probeContentType(imagePath.toPath());
            if (mimeType == null || !mimeType.startsWith("image/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file type. Only images are allowed.");
                return;
            }

            // Set response headers
            resp.setHeader("Cache-Control", "public, max-age=2592000"); // Cache for 1 month
            resp.setHeader("Content-Disposition", "inline");
            resp.setHeader("X-Content-Type-Options", "nosniff"); // Prevent MIME type guessing
            resp.setContentType(mimeType);

            try (FileInputStream fis = new FileInputStream(imagePath)) {
                byte[] buffer = new byte[4096];
                int bytes;
                while ((bytes = fis.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, bytes);
                }
            } catch (IOException e) {
                // Log the error with appropriate logging framework
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to serve image.");
            }
        }
    }
}

