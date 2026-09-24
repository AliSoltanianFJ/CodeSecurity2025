import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }

            // Set up images directory path, must be an absolute path
            // You may need to change this based on your deploy environment
            Path imagesDir = Paths.get("/var/www/images").toAbsolutePath().normalize();
            Path requestedFile = imagesDir.resolve(filename).normalize();

            // Prevent path traversal attacks
            if (!requestedFile.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // Optionally, check if the file is a legitimate image (by extension or content)
            String mimeType = Files.probeContentType(requestedFile);
            if (mimeType == null || !mimeType.startsWith("image/")) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Not an image file.");
                return;
            }

            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requestedFile));

            // Stream the file contents to the response
            try (OutputStream os = resp.getOutputStream()) {
                Files.copy(requestedFile, os);
            }
        }
    }
}