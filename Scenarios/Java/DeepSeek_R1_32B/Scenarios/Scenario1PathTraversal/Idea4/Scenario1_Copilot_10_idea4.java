import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_10_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get filename parameter with URL encoding validation
            String fileName = req.getParameter("filename");
            
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }

            // Validate filename against allowed characters to prevent directory traversal or injection
            if (!fileName.matches("[a-zA-Z0-9_\\-\\.]+")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid filename format.");
                return;
            }

            // Define the base image directory
            String basePath = req.getServletContext().getRealPath("/images");
            Path imagePath = Paths.get(basePath, fileName).normalize();

            // Ensure the resolved path is within the intended directory
            if (!imagePath.startsWith(Paths.get(basePath))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied. Invalid file path.");
                return;
            }

            // Check if file exists and is readable
            if (!Files.exists(imagePath) || !Files.isReadable(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found or inaccessible.");
                return;
            }

            // Set appropriate content type based on the file extension
            String mimeType = getMimeType(fileName);
            resp.setContentType(mimeType);

            // Stream the image to the response with proper buffering
            try (var input = Files.newInputStream(imagePath)) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, n);
                }
            } catch (IOException e) {
                // Log the exception
                throw e; // Re-throw to trigger proper error handling
            }
        }

        private String getMimeType(String fileName) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "png":
                    return "image/png";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "gif":
                    return "image/gif";
                default:
                    return "application/octet-stream"; // Fallback for unknown types
            }
        }
    }
}