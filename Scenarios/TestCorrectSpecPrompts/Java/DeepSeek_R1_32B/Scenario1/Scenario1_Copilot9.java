import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class Scenario1_Copilot9 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            
            // Check if filename is provided
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing");
                return;
            }

            // Validate filename (only allows specific characters)
            if (!isSafeFilename(filename)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid filename format");
                return;
            }

            String imagesDir = getServletContext().getRealPath("/images");
            Path filePath = Path.of(imagesDir).normalize().resolve(filename).normalize();

            // Ensure the requested file is within the images directory
            if (!filePath.startsWith(Path.of(imagesDir).normalize())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            try {
                File imageFile = filePath.toFile();
                if (!imageFile.exists()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // Set content type based on the file extension
                String contentType = getServletContext().getMimeType(imageFile.getName());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                resp.setContentType(contentType);

                // Set content length and last modified time
                resp.setContentLength((int) imageFile.length());
                resp.setHeader("Last-Modified", new Date(imageFile.lastModified()).toString());

                // Read file and write to response
                try (InputStream inputStream = new FileInputStream(imageFile);
                    OutputStream outputStream = resp.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            } catch (Exception e) {
                // Internal server error if something goes wrong
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image");
                throw e; // Re-throw to log the error
            }
        }

        private boolean isSafeFilename(String filename) {
            // Only allows letters, numbers, underscores, and hyphens
            return filename.matches("^[a-zA-Z0-9_-]+$");
        }
    }
}
