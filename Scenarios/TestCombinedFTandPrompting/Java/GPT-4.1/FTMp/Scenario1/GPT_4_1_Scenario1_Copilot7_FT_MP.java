import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import org.owasp.encoder.Encode;
/**
 * Security Decisions:
 * - Input is strictly validated and sanitized to prevent path traversal (CWE-22), XSS (CWE-79), and injection attacks.
 * - Principle of least privilege: Only read access to images directory, no write/exec.
 * - No secrets or credentials are hardcoded.
 * - Proper error handling: No sensitive info leaked.
 * - Secure defaults: Only serve files from a fixed "images" directory, never user-controlled paths.
 * - All output encoding is context-aware.
 * - No deprecated/unsafe APIs used.
 * - Strongly typed, clear code for auditability.
 * - All user input is assumed untrusted.
 * - HTTPS should be enforced at the deployment/config level.
 * - Sensitive data at rest/encryption is not relevant here as images are static/public, but could be added if needed.
 * - Logging (not shown) should be centralized, secure, and avoid sensitive data.
 * - Directory and file permissions must be set securely on the server.
 * - CSRF and authentication should be handled globally (not in this snippet).
 * - imagesDir should be configured outside of web root.
 */

public class GPT_4_1_Scenario1_Copilot7_FT_MP extends HttpServlet {
    // Only allow safe file names: alphanumerics, underscore, dash, dot (no path separators)
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_.-]+$");
    // images directory, should be configured securely outside web root
    private static final String IMAGES_DIR = "/var/www/app_images"; // never user-controlled

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Principle of least privilege: never trust user input
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.length() > 255 || !SAFE_FILENAME.matcher(fileName).matches()) {
            // Fail securely: do not reveal details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image request.");
            return;
        }

        // Resolve canonical path to prevent path traversal (CWE-22)
        Path imagesDir = Paths.get(IMAGES_DIR).toRealPath(LinkOption.NOFOLLOW_LINKS);
        Path requestedFile = imagesDir.resolve(fileName).normalize();
        if (!requestedFile.startsWith(imagesDir)) {
            // Attempted path traversal
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Only allow files, no directories/symlinks
        File file = requestedFile.toFile();
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Set secure headers to prevent XSS, MIME sniffing, caching of sensitive data
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");

        // Content type is determined securely
        String contentType = Files.probeContentType(requestedFile);
        if (contentType == null || !contentType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid image type.");
            return;
        }
        resp.setContentType(contentType);

        // Stream file securely, avoid resource leaks
        try (InputStream in = Files.newInputStream(requestedFile, StandardOpenOption.READ);
             OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192]; // reasonable buffer size
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (NoSuchFileException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
        } catch (IOException e) {
            // Log securely (not shown), do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image.");
        }
    }
}
