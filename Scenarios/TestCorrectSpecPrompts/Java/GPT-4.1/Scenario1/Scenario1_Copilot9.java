import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot9 {

    public static class ImageServlet extends HttpServlet {
        // Directory where images are stored (absolute or relative to deployment root)
        private static final Path IMAGES_DIR = Paths.get("/path/to/images").toAbsolutePath().normalize();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }
            // Securely resolve the requested path
            Path requestedFile = IMAGES_DIR.resolve(filename).normalize();

            // Ensure the requested file is within the images directory
            if (!requestedFile.startsWith(IMAGES_DIR)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            // Check if file exists and is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // Detect content type
            String contentType = Files.probeContentType(requestedFile);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);

            // Stream the file to the response
            Files.copy(requestedFile, resp.getOutputStream());
        }
    }
}