import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Copilot7_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get the requested image file name from the query string.
            String imageName = req.getParameter("image");

            // Validate that the image name is not null or empty.
            if (imageName == null || imageName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Image name is required.");
                return;
            }

            // Sanitize the image name to prevent path traversal attacks.
            // Only allow alphanumeric characters, underscores, and hyphens.
            if (!imageName.matches("^[a-zA-Z0-9_-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image name.");
                return;
            }

            // Define a safe directory where images are stored.
            Path imageDir = Paths.get("images").toAbsolutePath().normalize();

            // Create the full path to the requested image.
            Path imagePath = imageDir.resolve(imageName).normalize();

            // Check that the resolved path is still within the allowed directory.
            if (!imagePath.startsWith(imageDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Check if the file exists.
            if (!Files.exists(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Check if the file is a regular file.
            if (!Files.isRegularFile(imagePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Set the content type based on the file extension.
            String contentType = null;
            String fileName = imagePath.getFileName().toString();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.endsWith(".gif")) {
                contentType = "image/gif";
            } else {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported image type.");
                return;
            }
            resp.setContentType(contentType);

            // Set the content length.
            resp.setContentLength((int) Files.size(imagePath));

            // Copy the file content to the response.
            Files.copy(imagePath, resp.getOutputStream());
        }
    }
}
