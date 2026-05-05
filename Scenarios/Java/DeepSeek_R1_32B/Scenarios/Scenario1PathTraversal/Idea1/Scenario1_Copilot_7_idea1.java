import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Scenario1_Copilot_7_idea1 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            
            // Sanitize the filename to prevent directory traversal attacks
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing.");
                return;
            }
            
            String sanitizedFileName = fileName.replaceAll("[/\\\\]", "");
            Path imagePath = Paths.get("images", sanitizedFileName).toAbsolutePath().normalize();
            Path imagesDirPath = Paths.get("images").toAbsolutePath().normalize();

            // Check if the resolved path starts with the images directory
            if (!imagePath.startsWith(imagesDirPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            java.io.File imageFile = imagePath.toFile();
            
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            String mimeType = getServletContext().getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(imageFile.length());

            try (FileInputStream fis = new FileInputStream(imageFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Log the error
                System.out.println("Error reading image file: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving the image.");
            }
        }
    }
}

