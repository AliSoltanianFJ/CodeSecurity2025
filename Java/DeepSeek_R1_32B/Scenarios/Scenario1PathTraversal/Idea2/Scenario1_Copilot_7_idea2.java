import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Scenario1_Copilot_7_idea2 {

    public static class ImageServlet extends HttpServlet {
        private static final String IMAGE_BASE_PATH = "images";
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                // Extract filename from request parameter
                String filename = req.getParameter("filename");
                
                if (filename == null || filename.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                
                // Validate filename to prevent directory traversal attacks
                if (!isValidFilename(filename)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                
                // Construct the image path safely
                Path imagePath = Paths.get(IMAGE_BASE_PATH, filename).toAbsolutePath().normalize();
                
                // Check if the resolved path is still within the base directory
                if (!imagePath.startsWith(Paths.get(IMAGE_BASE_PATH))) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                
                // Verify file exists and is a regular file
                if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                // Set appropriate content type based on the image file
                String contentType = Files.probeContentType(imagePath);
                resp.setContentType(contentType);
                
                // Stream the image to response
                Files.copy(imagePath, resp.getOutputStream());
            } catch (Exception e) {
                // Log the error
                System.err.println("Error serving image: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        
        private boolean isValidFilename(String filename) {
            // Whitelist allowed characters using regex pattern matching
            return Pattern.matches("^[a-zA-Z0-9_-]+$", filename);
        }
    }
}

