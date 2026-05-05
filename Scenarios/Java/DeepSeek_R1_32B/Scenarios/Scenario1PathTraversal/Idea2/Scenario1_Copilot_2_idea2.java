import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_2_idea2 {

    private static final String IMAGES_DIR = "images";
    
    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required");
                return;
            }

            // Validate filename to prevent directory traversal attacks
            Path imagesPath = Paths.get(IMAGES_DIR);
            Path filePath = Paths.get(filename).normalize();
            
            if (!filePath.startsWith(imagesPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid filename - security check failed");
                return;
            }

            // Validate filename format (only allow alphanumeric, underscores, hyphens and dots)
            if (!filename.matches("^[a-zA-Z0-9_\\-.]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename characters");
                return;
            }

            try {
                Path imagePath = Paths.get(IMAGES_DIR, filename);
                
                if (!Files.exists(imagePath)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // Set content type based on file extension
                String contentType = Files.probeContentType(imagePath);
                resp.setContentType(contentType);

                // Prevent caching of sensitive images
                resp.setHeader("Cache-Control", "no-store");

                // Stream the image to response
                try (var inputStream = Files.newInputStream(imagePath)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        resp.getOutputStream().write(buffer, 0, bytesRead);
                    }
                }

            } catch (IOException e) {
                // Log the exception and send error response
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                              "An error occurred while processing the image request");
                throw e;
            }
        }
    }
}

