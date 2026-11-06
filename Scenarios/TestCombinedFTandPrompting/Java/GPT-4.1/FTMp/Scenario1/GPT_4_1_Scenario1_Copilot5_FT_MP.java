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
import java.nio.file.InvalidPathException;
import java.nio.file.LinkPermission;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.security.AccessController;
import java.security.PrivilegedAction;

// Security decisions and rationale:
// - All user input is strictly validated and sanitized to prevent path traversal, XSS, and injection (OWASP A1, A7).
// - Principle of least privilege: Only read access to a dedicated images directory, no write/exec.
// - No secrets or credentials are hardcoded.
// - Error handling avoids leaking sensitive info (CWE-209).
// - Strong logging, but no sensitive data in logs.
// - Only serves files with allowed image extensions (whitelist).
// - Symlinks are not followed (CWE-23).
// - Uses context-aware output encoding for headers.


public class GPT_4_1_Scenario1_Copilot5_FT_MP extends HttpServlet {

    private static final Logger logger = Logger.getLogger(GPT_4_1_Scenario1_Copilot5_FT_MP.class.getName());

    // Security: Do not hardcode secrets, this should be configured securely.
    // Images directory must be outside webroot to prevent direct access if misconfigured web server.
    private static final String IMAGES_DIR = "/var/app-secure/images"; // Must be configured securely, not world-readable

    // Only allow safe file names: no path separators, only alphanum, dash, underscore, dot
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]{1,100}$");
    // Whitelist allowed extensions
    private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Principle of least privilege: try-catch all, never leak stack traces
        String fileName = req.getParameter("file");

        // Validate input: null/empty check
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid file parameter.");
            return;
        }

        // Sanitize input: strict filename check
        if (!SAFE_FILENAME.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name format.");
            return;
        }

        // Extension whitelist check
        boolean allowed = false;
        String lowerName = fileName.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "File type not allowed.");
            return;
        }

        // Securely resolve path, prevent path traversal (CWE-22)
        Path imagesDir = Paths.get(IMAGES_DIR).toAbsolutePath().normalize();
        Path requestedFile;
        try {
            requestedFile = imagesDir.resolve(fileName).normalize();
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
            return;
        }

        // Check that the file is inside the images directory
        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Do not follow symlinks (CWE-23)
        if (Files.isSymbolicLink(requestedFile)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Symlinks not allowed.");
            return;
        }

        // Only serve files, not directories
        if (!Files.isRegularFile(requestedFile)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Principle of least privilege: Read file with minimal permissions
        try (FileInputStream fis = new FileInputStream(requestedFile.toFile())) {
            // Set secure headers to prevent XSS, MIME sniffing
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
            resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");

            // Content-Type: use secure detection
            String contentType = "application/octet-stream";
            try {
                String probe = Files.probeContentType(requestedFile);
                if (probe != null && probe.startsWith("image/")) {
                    contentType = probe;
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not determine content type", e);
            }
            resp.setContentType(contentType);

            // Context-aware output encoding for Content-Disposition
            String safeFileName = fileName.replaceAll("[\r\n\"\\\\]", "_"); // Prevent header injection
            resp.setHeader("Content-Disposition", "inline; filename=\"" + safeFileName + "\"");

            // Stream file securely
            OutputStream os = resp.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException ex) {
            // Log securely, no sensitive info to client
            logger.log(Level.WARNING, "Failed to serve image", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not serve image.");
        }
    }
}
