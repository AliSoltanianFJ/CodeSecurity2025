import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario1_Copilot2_FT_TOSP extends HttpServlet {
    private static final String IMAGES_DIR = "/opt/app/images"; // Use absolute path outside webroot
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario1_Copilot2_FT_TOSP.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");

        // Validate and sanitize input
        if (fileName == null || !SAFE_FILENAME.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        Path imagePath;
        try {
            imagePath = Paths.get(IMAGES_DIR, fileName).normalize();
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
            return;
        }

        // Prevent path traversal
        Path imagesDirPath = Paths.get(IMAGES_DIR).toAbsolutePath().normalize();
        if (!imagePath.toAbsolutePath().startsWith(imagesDirPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Only serve files, not directories
        File imageFile = imagePath.toFile();
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "private, max-age=604800, must-revalidate"); // 1 week
        resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName.replace("\"", "") + "\"");

        // Detect and set MIME type safely
        String mimeType = Files.probeContentType(imagePath);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file type.");
            return;
        }
        resp.setContentType(mimeType);
        resp.setContentLengthLong(imageFile.length());

        // Stream file with buffer, handle errors securely
        try (OutputStream out = resp.getOutputStream();
             FileInputStream in = new FileInputStream(imageFile)) {
            byte[] buffer = new byte[8192]; // Safe buffer size
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error serving image: {0}", ex.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file.");
        }
    }
}
