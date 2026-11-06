import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.LinkOption;
import java.io.OutputStream;

// Security Decisions:
// - All user input is strictly validated and sanitized to prevent path traversal (CWE-22), XSS (CWE-79), and injection.
// - Only files within the "images" directory are accessible (principle of least privilege).
// - No sensitive information is leaked in error messages (CWE-209).
// - All streams are safely closed, and no deprecated/unsafe APIs are used.
// - Assumes HTTPS is enforced at the server configuration level (do not serve over HTTP).
// - No secrets or credentials are hardcoded.

public class GPT_4_1_Scenario1_Copilot9_FT_MP extends HttpServlet {
    private static final String IMAGES_DIR = "images";

    // Only allow safe file names: alphanumeric, underscore, hyphen, dot, but not starting/ending with dot or having ".."
    private static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.length() < 1 || fileName.length() > 255) return false;
        if (fileName.contains("..")) return false;
        if (fileName.startsWith(".") || fileName.endsWith(".")) return false;
        return fileName.matches("^[a-zA-Z0-9_\-\.]+$");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        
        // Validate and sanitize user input
        if (!isValidFileName(fileName)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Resolve the canonical path securely to prevent path traversal
        Path imagesDir = Paths.get(IMAGES_DIR).toAbsolutePath().normalize();
        Path requestedFile = imagesDir.resolve(fileName).normalize();

        // Ensure the requested file is inside the images directory
        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check if the file exists and is a regular file
        if (!Files.exists(requestedFile, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(requestedFile, LinkOption.NOFOLLOW_LINKS)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set secure headers to prevent XSS and MIME sniffing
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "private, max-age=31536000, immutable");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");

        // Detect and set the content type securely
        String contentType = Files.probeContentType(requestedFile);
        if (contentType == null || !contentType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file type.");
            return;
        }
        resp.setContentType(contentType);

        // Stream the file securely
        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(requestedFile, out);
            out.flush();
        } catch (IOException e) {
            // Log securely (do not leak details to client)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve the image.");
        }
    }
}
