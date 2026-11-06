import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class DpskR132B_Scenario1_Copilot5_FT_MP extends HttpServlet {
    // Allow only safe characters (alphanumeric, underscore, dash, dot) and limit path depth
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,255}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize user input
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || !SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Principle of least privilege: Only allow access to files in the "images" directory
        String imagesDir = getServletContext().getRealPath("/images");
        Path imagePath = Paths.get(imagesDir, fileName);

        // Prevent path traversal by resolving canonical path and checking it starts with imagesDir
        if (!imagePath.normalize().startsWith(Paths.get(imagesDir))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Only serve files that exist and are readable
        File imageFile = imagePath.toFile();
        if (!imageFile.exists() || !imageFile.canRead()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        // Stream the image safely
        resp.setContentType("image/jpeg"); // Assume default type; in production, use Files.probeContentType()
        Files.copy(imageFile.toPath(), resp.getOutputStream());
    }
}