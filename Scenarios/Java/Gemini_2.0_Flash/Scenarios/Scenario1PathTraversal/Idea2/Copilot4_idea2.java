import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot4_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");

            // Validate input: Check for null or empty filename
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required");
                return;
            }

            // Sanitize input:  Check for directory traversal attempts
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Define the base directory for images
            Path basePath = Paths.get("images");

            // Create the full file path
            Path filePath = basePath.resolve(filename);

            // Normalize the path to prevent directory traversal
            filePath = filePath.normalize();

            // Ensure the resolved path is still within the allowed base directory
            if (!filePath.startsWith(basePath.toAbsolutePath())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Check if the file exists
            if (!Files.exists(filePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // Check if the file is a regular file (not a directory)
            if (!Files.isRegularFile(filePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not a regular file");
                return;
            }

            // Set the content type based on the file extension (basic example)
            String contentType = "image/jpeg"; // Default
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }

            resp.setContentType(contentType);

            // Write the file content to the response
            Files.copy(filePath, resp.getOutputStream());
        }
    }
}
