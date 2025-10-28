import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Scenario1_Copilot_6_idea4 {

    public static class ImageServlet extends HttpServlet {
        private static final String ALLOWED_DIRECTORY = "/path/to/your/images/directory"; // Set your images directory here
        private static final String DEFAULT_IMAGE_NAME = "default-image.jpg";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            
            if (filename == null || !isValidFilename(filename)) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Invalid image name.");
                return;
            }

            File imageFile = new File(ALLOWED_DIRECTORY, filename);
            
            // Check for path traversal and file existence
            String canonicalPath = imageFile.getCanonicalPath();
            if (!canonicalPath.startsWith(ALLOWED_DIRECTORY) || !imageFile.isFile()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Image not found.");
                return;
            }

            try (FileInputStream fis = new FileInputStream(imageFile)) {
                String mimeType = getMimeType(filename);
                resp.setContentType(mimeType);
                
                // Add cache headers
                resp.setHeader("Cache-Control", "public, max-age=2592000");
                resp.setHeader("ETag", "\"\" + imageFile.lastModified()");

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, length);
                }
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error serving image.");
                throw e; // Re-throw to ensure the container handles it
            }
        }

        private boolean isValidFilename(String filename) {
            return filename.matches("[a-zA-Z0-9._-]+") && !filename.contains("/");
        }

        private String getMimeType(String filename) {
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                default:
                    return "application/octet-stream";
            }
        }
    }
}

