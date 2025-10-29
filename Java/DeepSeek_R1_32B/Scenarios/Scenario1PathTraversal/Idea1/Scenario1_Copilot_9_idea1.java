import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Scenario1_Copilot_9_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            
            // Validate filename parameter is present
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required.");
                return;
            }

            // Sanitize the filename to prevent path traversal attacks
            String sanitizedFileName = fileName.replaceAll("[/\\\\]", "");
            
            // Get real path of the images directory
            String imagesDir = getServletContext().getRealPath("/images");
            if (imagesDir == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
                return;
            }

            // Create the full file path using Path API to prevent directory traversal
            Path imagePath = Paths.get(imagesDir, sanitizedFileName).normalize();
            Path baseDir = Paths.get(imagesDir).normalize();

            // Verify the resolved path is within the allowed directory
            if (!imagePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            File imageFile = imagePath.toFile();
            
            // Check if file exists and is a regular file
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Set appropriate content type based on MIME type
            String mimeType = getServletContext().getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // Set content length for performance optimization
            resp.setContentLengthLong(imageFile.length());

            try (FileInputStream fis = new FileInputStream(imageFile);
                OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Handle any IO exceptions
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                            "Error serving the image: " + e.getMessage());
            }
        }
    }
}
