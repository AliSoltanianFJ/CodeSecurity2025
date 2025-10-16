import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot8_idea2 {

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

            // Sanitize input: Prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Define the base directory for images
            Path baseDir = Paths.get("images").toAbsolutePath().normalize();
            Path imagePath = baseDir.resolve(filename).normalize();

            // Prevent path traversal: Ensure the resolved path is within the base directory
            if (!imagePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // Security: Check if the file exists
            if (!Files.exists(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Security: Check if the file is readable
            if (!Files.isReadable(imagePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Image not readable");
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

            // Write the image data to the response
            Files.copy(imagePath, resp.getOutputStream());
        }
    }
}
