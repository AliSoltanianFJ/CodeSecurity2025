import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario1_Copilot10_FT_TOSP extends HttpServlet {
    private static final String IMAGE_DIR = "images";
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize input
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || !FILENAME_PATTERN.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Prevent path traversal and buffer overflow by using safe APIs and limiting file name length
        if (fileName.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File name too long.");
            return;
        }

        Path imagePath = Paths.get(IMAGE_DIR, fileName).normalize();

        // Ensure the image path is within the images directory
        Path canonicalImageDir = Paths.get(IMAGE_DIR).toAbsolutePath().normalize();
        Path canonicalImagePath = imagePath.toAbsolutePath().normalize();
        if (!canonicalImagePath.startsWith(canonicalImageDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check if file exists and is a regular file
        if (!Files.exists(canonicalImagePath) || !Files.isRegularFile(canonicalImagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Set content type safely
        String contentType = Files.probeContentType(canonicalImagePath);
        if (contentType == null || !contentType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Only image files are supported.");
            return;
        }
        resp.setContentType(contentType);

        // Stream the image securely
        try (InputStream in = Files.newInputStream(canonicalImagePath);
            OutputStream out = resp.getOutputStream()) {
            // Use a fixed-size buffer to prevent buffer overflow
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image.");
        }
    }
}
