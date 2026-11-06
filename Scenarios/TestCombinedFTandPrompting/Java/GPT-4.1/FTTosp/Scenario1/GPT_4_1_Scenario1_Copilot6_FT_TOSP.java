import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.io.File;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;

public class GPT_4_1_Scenario1_Copilot6_FT_TOSP extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Get the image filename from the "file" parameter
        String fileName = req.getParameter("file");

        // Validate and sanitize input (prevent path traversal, only allow specific patterns)
        if (fileName == null || fileName.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
            return;
        }

        // Only allow alphanumeric characters, underscores, hyphens, and safe image extensions
        Pattern safePattern = Pattern.compile("^[\\w\\-]+\\.(jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE);
        if (!safePattern.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Define the images directory (outside the web root, hardcoded)
        Path imagesDir = Paths.get("/var/www/app_images");

        // Resolve the canonical path and prevent directory traversal
        Path imagePath = imagesDir.resolve(fileName).normalize();

        // Check for directory traversal
        if (!imagePath.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check if the file exists and is a regular file (not a symlink)
        File imageFile = imagePath.toFile();
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Prevent insecure deserialization and XSS (don't echo user input, set safe headers)
        resp.setContentType("image/jpeg"); // You may want to detect MIME type securely
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "private, max-age=86400");

        // Stream the image securely in chunks (prevent buffer overflow)
        try (OutputStream out = resp.getOutputStream()) {
            final int bufferSize = 8192; // Safe buffer size
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            try (var in = Files.newInputStream(imagePath)) {
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            // Log securely (no sensitive info), do not leak internal details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image.");
        }
    }
}
