import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot3 {

    public static class ImageServlet extends HttpServlet {
        // You may want to set this to the absolute path of your images directory
        // For example: "/var/www/images" or getServletContext().getRealPath("/images")
        // Here we use a path relative to your deployment directory.
        private final String IMAGES_DIR = "images";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");

            if (fileName == null || fileName.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }

            // Ensure no path traversal by normalizing and checking the image folder root
            Path imagesRoot = Paths.get(IMAGES_DIR).toAbsolutePath().normalize();
            Path requestedFile = imagesRoot.resolve(fileName).normalize();

            // Check: requestedFile must start with imagesRoot
            if (!requestedFile.startsWith(imagesRoot)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
                return;
            }

            // Check if file exists and is a file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // Attempts to determine content type
            String contentType = Files.probeContentType(requestedFile);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);

            // Set appropriate caching headers if desired
            // resp.setHeader("Cache-Control", "max-age=86400");

            // Stream the file to the response
            try {
                Files.copy(requestedFile, resp.getOutputStream());
            } catch (IOException e) {
                // Do not expose stack trace to user
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve file");
            }
        }
    }
}