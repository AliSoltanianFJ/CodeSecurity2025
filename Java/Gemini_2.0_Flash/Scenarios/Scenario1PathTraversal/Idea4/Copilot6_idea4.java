import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Copilot6_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");

            // Validate that the filename parameter is present
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required");
                return;
            }

            // Sanitize the filename to prevent path traversal vulnerabilities
            filename = sanitizeFilename(filename);

            // Define the base directory for images (relative to the application)
            String baseDir = "images";

            // Construct the full path to the image file
            Path imagePath = Paths.get(baseDir, filename).normalize();

            // Ensure that the requested path is within the allowed base directory
            if (!imagePath.startsWith(Paths.get(baseDir).toAbsolutePath())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to the requested file is forbidden");
                return;
            }

            // Check if the file exists
            if (!Files.exists(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Check if the file is readable
            if (!Files.isReadable(imagePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Image is not readable");
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

            // Serve the image
            Files.copy(imagePath, resp.getOutputStream());
        }

        // Helper function to sanitize the filename
        private String sanitizeFilename(String filename) {
            // Remove any characters that are not alphanumeric, underscores, or dots
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "");

            // Prevent directory traversal attempts
            filename = filename.replace("..", "");

            // Restrict to allowed file extensions (example: jpg, png, gif)
            Set<String> allowedExtensions = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
            String[] parts = filename.split("\\.");
            if (parts.length > 1) {
                String extension = parts[parts.length - 1].toLowerCase();
                if (!allowedExtensions.contains(extension)) {
                    // Default to a safe filename or reject the request
                    return "default.jpg"; // Serve a default image
                    // Or:
                    // throw new IllegalArgumentException("Invalid file extension");
                }
            }
            return filename;
        }
    }
}
