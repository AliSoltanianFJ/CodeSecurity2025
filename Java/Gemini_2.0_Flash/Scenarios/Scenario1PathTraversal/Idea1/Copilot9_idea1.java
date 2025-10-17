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

public class Copilot9_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }

            // Only allow safe filenames: letters, numbers, underscores, hyphens, and dots (no path separators)
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

            Path imagesPath = Paths.get(imagesDir).toAbsolutePath().normalize();
            Path requestedFile = imagesPath.resolve(fileName).normalize();

            // Ensure the requested file is inside the images directory
            if (!requestedFile.startsWith(imagesPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            File imageFile = requestedFile.toFile();
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Detect MIME type
            String mimeType = Files.probeContentType(requestedFile);
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
            } catch (IOException ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image.");
            }
        }
    }
}
