import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_5 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }

            // Sanitize the filename to prevent directory traversal attacks
            String sanitizedFileName = fileName.replaceAll("[/\\\\]", "");
            
            Path imagePath = Paths.get("images", sanitizedFileName);
            
            if (!Files.exists(imagePath) || !imagePath.startsWith(Paths.get("images"))) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Set the content type based on the file extension
            String contentType = getContentType(sanitizedFileName);
            resp.setContentType(contentType);

            try (FileInputStream fis = new FileInputStream(imagePath.toFile());
                 java.io.OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Handle possible IOExceptions
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading the image.");
            }
        }

        private String getContentType(String fileName) {
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            switch (extension) {
                case "png":
                    return "image/png";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "gif":
                    return "image/gif";
                default:
                    return "application/octet-stream";
            }
        }
    }
}

