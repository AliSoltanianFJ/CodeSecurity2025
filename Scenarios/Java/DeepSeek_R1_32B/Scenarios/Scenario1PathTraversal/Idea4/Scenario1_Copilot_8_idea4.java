import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Scenario1_Copilot_8_idea4 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get the filename from request parameters
            String fileName = req.getParameter("filename");
            
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Validate and sanitize the filename to prevent path traversal attacks
            String sanitizedName = validateAndSanitizeFileName(fileName);
            if (sanitizedName == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // Construct the full file path safely
            String imagesDir = getServletContext().getRealPath("/images/");
            File imageFile = new File(imagesDir + sanitizedName);

            if (!imageFile.isFile() || !imageFile.exists()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Set the appropriate MIME type based on the file extension
            String mimeType = getServletContext().getMimeType(imageFile.getName());
            resp.setContentType(mimeType);

            // Add cache control headers to prevent caching issues
            resp.setHeader("Cache-Control", "public, max-age=31536000");

            try (FileInputStream fis = new FileInputStream(imageFile);
                 java.io.OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Log the exception
                getServletContext().log("Error serving image", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        private String validateAndSanitizeFileName(String fileName) {
            if (fileName == null || fileName.trim().isEmpty()) {
                return null;
            }

            // Check for directory traversal attempts
            if (fileName.contains("..") || fileName.startsWith("/") || fileName.contains("%2f")) {
                return null;
            }

            // Keep only alphanumeric characters and allowed extensions
            String allowedExtensions = "(png|jpg|jpeg|gif|bmp)";
            String regexPattern = "^[a-zA-Z0-9_-]+\\." + allowedExtensions + "$";
            
            if (!fileName.matches(regexPattern)) {
                return null;
            }

            // Replace any potential encoded directory traversal attempts
            fileName = java.net.URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8);
            
            return fileName;
        }
    }
}

