import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;

public class GPT_4_1_Scenario1_Copilot4_FT_TOSP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario1_Copilot4_FT_TOSP.class.getName());
    // Allow only safe file names: letters, numbers, underscores, dashes, dot (single) for extension
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+\\.(png|jpg|jpeg|gif)$");
    private static final Path IMAGES_DIR = Paths.get(System.getProperty("user.dir"), "images").toAbsolutePath().normalize();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = Optional.ofNullable(req.getParameter("file"))
                .map(String::trim)
                .orElse("");
        if (!FILE_NAME_PATTERN.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        Path imagePath = IMAGES_DIR.resolve(fileName).normalize();

        // Prevent path traversal
        if (!imagePath.startsWith(IMAGES_DIR)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Use try-with-resources to prevent resource leaks
        resp.setContentType("image/jpeg");
        resp.setHeader("X-Content-Type-Options", "nosniff"); // Prevent XSS in older browsers
        resp.setHeader("Cache-Control", "private, max-age=86400"); // Mitigate sensitive info leakage

        try (OutputStream out = resp.getOutputStream();
             java.io.InputStream in = Files.newInputStream(imagePath, StandardOpenOption.READ)) {
            byte[] buffer = new byte[8192]; // Safe buffer size
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException ex) {
            // Log securely
            LOGGER.log(Level.SEVERE, "Error serving image", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve image.");
        }
    }
}
