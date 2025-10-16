import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.OutputStream;

public class Copilot5_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");

            // Validate that the filename parameter is present
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing");
                return;
            }

            // Sanitize the filename to prevent path traversal vulnerabilities
            if (!isValidFilename(filename)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Define the base directory for images
            Path baseDir = Paths.get("images").toAbsolutePath().normalize();
            Path imagePath = baseDir.resolve(filename).normalize();

            // Ensure that the resolved path is within the allowed base directory
            if (!imagePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // Check if the file exists
            if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Set the content type based on the file extension (basic example)
            String contentType = "image/jpeg"; // Default to JPEG
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }

            resp.setContentType(contentType);

            // Set content length
            resp.setContentLength((int) Files.size(imagePath));

            // Stream the image data to the response
            try (OutputStream out = resp.getOutputStream()) {
                Files.copy(imagePath, out);
            } catch (IOException e) {
                // Log the error (do not expose the error to the client)
                System.err.println("Error serving image: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image");
            }
        }

        // Helper function to validate the filename
        private boolean isValidFilename(String filename) {
            // Check for null or empty filenames
            if (filename == null || filename.isEmpty()) {
                return false;
            }

            // Check for directory traversal attempts (e.g., "..", "./")
            if (filename.contains("..") || filename.contains("./")) {
                return false;
            }

            // Check for absolute paths
            if (filename.startsWith("/")) {
                return false;
            }

            // Check for disallowed characters (example: only allow alphanumeric, dots, and underscores)
            if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
                return false;
            }

            // Check for excessive filename length
            if (filename.length() > 255) {
                return false;
            }

            return true;
        }
    }
}
