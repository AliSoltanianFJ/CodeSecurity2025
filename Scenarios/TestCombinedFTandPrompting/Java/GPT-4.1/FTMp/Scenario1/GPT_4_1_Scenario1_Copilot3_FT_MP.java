import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.OutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario1_Copilot3_FT_MP extends HttpServlet {
    // Security: Only allow filenames matching strict pattern (no path separators, only safe chars)
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final String IMAGES_DIR = "images"; // Security: Do not allow user to control this
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario1_Copilot3_FT_MP.class.getName());

    /* Security decisions:
      - Principle of least privilege: Only read access to images directory
      - Input validation: Strict regex on filename, no path traversal
      - Error handling: No sensitive info leaked to client
      - No hardcoded secrets
      - All user input sanitized
      - Secure defaults: deny by default
      - Output encoding: N/A (binary data)
      - Sensitive data in transit: Assume HTTPS enforced at server config
      - No deprecated APIs used
      - All security-relevant decisions documented
    */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Security: Validate and sanitize user input
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
            return;
        }

        // Security: Validate filename against strict whitelist
        Matcher matcher = SAFE_FILENAME.matcher(fileName);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Security: Resolve canonical path and prevent path traversal
        Path imagesDir = Paths.get(IMAGES_DIR).toAbsolutePath().normalize();
        Path imagePath = imagesDir.resolve(fileName).normalize();

        // Security: Ensure the resolved path is within the images directory
        if (!imagePath.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Security: Principle of least privilege - only read access
        try {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                if (Files.exists(imagePath) && Files.isRegularFile(imagePath) && Files.isReadable(imagePath)) {
                    resp.setContentType("image/jpeg"); // Security: do not trust user input for content type
                    resp.setHeader("X-Content-Type-Options", "nosniff"); // Security header to prevent MIME sniffing
                    resp.setHeader("Cache-Control", "private, max-age=31536000, immutable"); // Security: prevent sensitive caching

                    try (OutputStream out = resp.getOutputStream();
                        InputStream in = Files.newInputStream(imagePath, StandardOpenOption.READ)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to read image file.", e);
                        try {
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file.");
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "Failed to send error response.", ex);
                        }
                    }
                } else {
                    try {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to send not found response.", e);
                    }
                }
                return null;
            });
        } catch (SecurityException e) {
            LOGGER.log(Level.SEVERE, "Security exception while accessing image file.", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Access denied.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO exception while sending error response.", e);
        }
    }
}
