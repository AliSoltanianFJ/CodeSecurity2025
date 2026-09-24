import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot10 {

    public static class ImageServlet extends HttpServlet {
        // Relative to your web app's working directory, typically "images"
        private static final String IMAGE_DIR = "images";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");

            // Basic validation of the fileName parameter
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File parameter required.");
                return;
            }

            // Resolve the image directory and requested file paths
            Path imageDir = Paths.get(IMAGE_DIR).toAbsolutePath().normalize();
            Path requestedFile;
            try {
                requestedFile = imageDir.resolve(fileName).normalize().toAbsolutePath();
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }

            // Prevent path traversal
            if (!requestedFile.startsWith(imageDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Check if file exists and is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // Set Content-Type based on file extension
            String mimeType = getServletContext().getMimeType(requestedFile.getFileName().toString());
            if (mimeType == null) mimeType = "application/octet-stream";
            resp.setContentType(mimeType);

            // Output the file
            resp.setContentLengthLong(Files.size(requestedFile));
            try (OutputStream os = resp.getOutputStream()) {
                Files.copy(requestedFile, os);
            }
        }
    }
}