import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario1_Copilot_6_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("filename");
            
            // Validate and sanitize filename input
            if (fileName == null || fileName.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required");
                return;
            }
            
            // Check for invalid characters in filename
            if (!fileName.matches("[a-zA-Z0-9._-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }
            
            // Prevent path traversal attacks
            Path imagePath = Paths.get("images", fileName).normalize();
            String realPath = getServletContext().getRealPath(imagePath.toString());
            
            File imageFile = new File(realPath);
            
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }
            
            // Set appropriate MIME type
            String mimeType = getServletContext().getMimeType(imageFile.getName());
            resp.setContentType(mimeType);
            
            // Serve the image
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                byte[] buffer = new byte[1024];
                int length;
                
                while ((length = fis.read(buffer)) > 0) {
                    resp.getOutputStream().write(buffer, 0, length);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image");
            }
        }
    }
}

