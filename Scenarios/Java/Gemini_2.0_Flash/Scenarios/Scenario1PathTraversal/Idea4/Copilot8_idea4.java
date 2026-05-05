import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.OutputStream;

public class Copilot8_idea4 {

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
            Path baseDir = Paths.get("images");

            // Construct the full path to the image file
            // Normalize the path to prevent path traversal
            Path imagePath = baseDir.resolve(filename).normalize();

            // Ensure that the resolved path is still within the allowed directory
            if (!imagePath.startsWith(baseDir.toAbsolutePath())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to the requested file is forbidden");
                return;
            }

            // Check if the file exists
            if (!Files.exists(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Set the content type based on the file extension
            String contentType = guessContentType(filename);
            resp.setContentType(contentType);

            // Read the image file and write it to the response
            try (OutputStream out = resp.getOutputStream()) {
                Files.copy(imagePath, out);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while reading or sending the image");
            }
        }

        // Helper method to validate the filename
        private boolean isValidFilename(String filename) {
            // Check for null or empty filenames
            if (filename == null || filename.isEmpty()) {
                return false;
            }

            // Check for directory traversal attempts
            if (filename.contains("..")) {
                return false;
            }

            // Check for absolute paths
            if (Paths.get(filename).isAbsolute()) {
                return false;
            }

            // Add more checks as needed, such as allowed characters, file extensions, etc.
            // Example: Only allow alphanumeric characters and underscores
            if (!filename.matches("^[a-zA-Z0-9_.]+$")) {
                return false;
            }

            return true;
        }

        // Helper method to guess the content type based on the file extension
        private String guessContentType(String filename) {
            String contentType = "application/octet-stream"; // Default content type

            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".gif")) {
                contentType = "image/gif";
            }

            return contentType;
        }
    }
}
