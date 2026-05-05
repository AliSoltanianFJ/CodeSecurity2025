import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_9_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("filename");
            
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required");
                return;
            }
            
            // Validate filename to prevent directory traversal attacks
            if (!fileName.matches("^[^/]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename format");
                return;
            }
            
            String imagesDir = getServletContext().getRealPath("/images/");
            Path filePath = Paths.get(imagesDir, fileName).normalize();
            
            // Check if the resolved path is within the allowed directory
            if (!filePath.startsWith(Paths.get(imagesDir))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path");
                return;
            }
            
            // Check if file exists and is a regular file
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }
            
            // Set headers to prevent caching and specify content type
            resp.setHeader("Content-Security-Policy", "default-src 'self'");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            
            try {
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null || !isSupportedImageFormat(mimeType)) {
                    throw new IOException("Unsupported image format");
                }
                resp.setContentType(mimeType);
                
                // Read the file and write to response output stream
                byte[] bytes = Files.readAllBytes(filePath);
                resp.getOutputStream().write(bytes);
            } catch (IOException e) {
                throw new RuntimeException("Error serving image", e);
            }
        }
        
        private boolean isSupportedImageFormat(String mimeType) {
            return switch(mimeType) {
                case "image/jpeg" -> true;
                case "image/png" -> true;
                case "image/gif" -> true;
                case "image/webp" -> true;
                default -> false;
            };
        }
    }
}

