import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_7_idea4 {

    public static class ImageServlet extends HttpServlet {
        private static final String ALLOWED_CHARACTERS = "[a-zA-Z0-9_\\-\\.]+";
        private static final String IMAGES_FOLDER = "images";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Retrieve the filename parameter
            String fileName = req.getParameter("filename");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required");
                return;
            }

            // Check for allowed characters to prevent path traversal
            if (!fileName.matches(ALLOWED_CHARACTERS)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename format");
                return;
            }

            try {
                // Construct the full file path safely
                File imagesDir = new File(IMAGES_FOLDER);
                Path imagePath = Paths.get(imagesDir.getAbsolutePath(), fileName).normalize();

                // Check if the resolved path is still within the allowed directory
                if (!imagePath.startsWith(imagesDir.toPath())) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file access attempt");
                    return;
                }

                File imageFile = imagePath.toFile();
                
                if (!imageFile.exists()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                if (imageFile.isDirectory()) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot access directories");
                    return;
                }

                // Set appropriate content type based on file extension
                String mimeType = getMimeType(imageFile.getName());
                if (mimeType == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Unsupported image format");
                    return;
                }

                resp.setContentType(mimeType);
                
                // Stream the file to response output
                try (FileInputStream fis = new FileInputStream(imageFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        resp.getOutputStream().write(buffer, 0, length);
                    }
                }

            } catch (Exception e) {
                // Log the error with proper logging framework
                System.err.println("Error serving image: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        private String getMimeType(String fileName) {
            String mimeType = null;
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            
            switch(extension) {
                case "jpg":
                case "jpeg":
                    mimeType = "image/jpeg";
                    break;
                case "gif":
                    mimeType = "image/gif";
                    break;
                case "png":
                    mimeType = "image/png";
                    break;
                // Add other supported formats as needed
                default:
                    mimeType = null;
            }
            
            return mimeType;
        }
    }
}

