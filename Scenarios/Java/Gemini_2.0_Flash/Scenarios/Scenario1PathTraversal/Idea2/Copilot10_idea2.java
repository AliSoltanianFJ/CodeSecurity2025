import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.*;

public class Copilot10_idea2 {

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

            // Sanitize input:  Check for path traversal attempts
            if (filename.contains("..") || filename.startsWith("/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Define the base directory for images
            Path basePath = Paths.get("images").toAbsolutePath().normalize();
            Path imagePath = basePath.resolve(filename).normalize();

            // Check that the resolved path is still within the base directory
            if (!imagePath.startsWith(basePath)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Attempt to read the file
            try {
                byte[] imageData = Files.readAllBytes(imagePath);

                // Determine content type (example: using Files.probeContentType)
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "application/octet-stream"; // Default type
                }

                // Set content type and write data to response
                resp.setContentType(contentType);
                resp.setContentLength(imageData.length);
                resp.getOutputStream().write(imageData);
            } catch (NoSuchFileException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image");
            }
        }
    }
}
