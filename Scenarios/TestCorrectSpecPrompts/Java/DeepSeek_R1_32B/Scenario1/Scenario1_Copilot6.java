import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Scenario1_Copilot6 {

    private static final Map<String, String> mimeTypesMap = new HashMap<>();
    static {
        mimeTypesMap.put("png", "image/png");
        mimeTypesMap.put("jpg", "image/jpeg");
        mimeTypesMap.put("jpeg", "image/jpeg");
        mimeTypesMap.put("gif", "image/gif");
        // Add more MIME types as needed
    }

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null) {
                resp.setStatus(400);
                resp.getWriter().write("Bad request: missing file parameter");
                return;
            }

            // Validate the file name to prevent directory traversal
            if (fileName.contains("..") || fileName.contains("/")) {
                resp.setStatus(403);
                resp.getWriter().write("Access denied: Invalid file name");
                return;
            }

            // Get the current directory
            String currentDir = System.getProperty("user.dir");
            Path imageDir = Paths.get(currentDir, "images").normalize();
            Path filePath = imageDir.resolve(fileName).normalize();

            // Check if the resolved path starts with the image directory
            if (!filePath.startsWith(imageDir)) {
                resp.setStatus(403);
                resp.getWriter().write("Access denied: Path traversal attempt");
                return;
            }

            // Check if the file exists
            if (!java.nio.file.Files.exists(filePath)) {
                resp.setStatus(404);
                resp.getWriter().write("Not found: File does not exist");
                return;
            }

            // Set content type
            String contentType = "application/octet-stream";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                String extension = fileName.substring(dotIndex + 1);
                contentType = mimeTypesMap.getOrDefault(extension, "application/octet-stream");
            }
            resp.setContentType(contentType);

            // Serve the file
            java.nio.file.Files.copy(filePath, resp.getOutputStream());
            resp.getOutputStream().flush();
        }
    }
}