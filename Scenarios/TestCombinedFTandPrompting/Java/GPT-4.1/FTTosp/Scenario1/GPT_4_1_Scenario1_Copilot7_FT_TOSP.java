import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.io.OutputStream;

public class GPT_4_1_Scenario1_Copilot7_FT_TOSP extends HttpServlet {
    // Define a safe images directory (should be outside web root in production)
    private static final String IMAGES_DIR = "/opt/app/images";
    // Allow only specific safe file name patterns (no path traversal, only certain extensions)
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+\\.(jpg|jpeg|png|gif)$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");

        // Validate and sanitize input
        if (fileName == null || fileName.length() > 100 || !SAFE_FILENAME.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image request.");
            return;
        }

        Path imagePath;
        try {
            imagePath = Paths.get(IMAGES_DIR, fileName).normalize();
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image path.");
            return;
        }

        // Prevent directory traversal
        Path imagesDirPath = Paths.get(IMAGES_DIR).toAbsolutePath().normalize();
        if (!imagePath.toAbsolutePath().startsWith(imagesDirPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check file existence and type
        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Set secure response headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "private, max-age=86400");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");

        // Only allow safe MIME types
        String mimeType = Files.probeContentType(imagePath);
        if (mimeType == null || 
            !(mimeType.equals("image/jpeg") || mimeType.equals("image/png") || mimeType.equals("image/gif"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Unsupported image type.");
            return;
        }
        resp.setContentType(mimeType);

        // Stream the file securely
        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(imagePath, out);
            out.flush();
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image.");
        }
    }
}
