import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot2_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file name.");
                return;
            }

            // Only allow filenames with safe characters (alphanumeric, underscore, dash, dot)
            if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Define the images directory (relative to webapp root)
            String imagesDir = getServletContext().getRealPath("/images");
            if (imagesDir == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
                return;
            }

            // Resolve the canonical path and ensure it's inside the images directory
            Path imagesPath = Paths.get(imagesDir).toAbsolutePath().normalize();
            Path requestedPath = imagesPath.resolve(fileName).normalize();

            if (!requestedPath.startsWith(imagesPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            File imageFile = requestedPath.toFile();
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Detect MIME type
            String mimeType = Files.probeContentType(requestedPath);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            resp.setContentLengthLong(imageFile.length());

            try (FileInputStream fis = new FileInputStream(imageFile);
                 OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image.");
            }
        }
    }
}
