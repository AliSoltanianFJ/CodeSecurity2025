import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String imagesDir = "images"; // The directory containing images (relative to app working dir)
            String fileName = req.getParameter("file"); // GET variable 'file'
            
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }
            
            // Construct target path safely
            Path imagesBase = Paths.get(imagesDir).toAbsolutePath().normalize();
            Path requestedFile = imagesBase.resolve(fileName).normalize();

            // Check if the resolved path is still under the imagesBase directory (prevent path traversal)
            if (!requestedFile.startsWith(imagesBase)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            // Check if the file exists and is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Optionally, detect the content type
            String contentType = Files.probeContentType(requestedFile);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);

            // Stream the image bytes to response
            try (OutputStream os = resp.getOutputStream()) {
                Files.copy(requestedFile, os);
            } catch (IOException e) {
                // Do not leak stack trace info to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not deliver file.");
            }
        }
    }
}