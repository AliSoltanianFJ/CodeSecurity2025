import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.LinkOption;
import java.util.regex.Pattern;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario1_Copilot1_FT_MP extends HttpServlet {
    // Security decision: Use a strict allowlist for file names to prevent path traversal (CWE-22)
    // and only serve files from a dedicated, non-writable directory.

    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario1_Copilot1_FT_MP.class.getName());

    // Security decision: Store the absolute path to the images directory in a secure configuration,
    // never hardcode secrets or credentials.
    private static final String IMAGES_DIR = "/var/www/app_images"; // Principle of least privilege: this dir should be read-only for the app user

    // Security decision: Only allow filenames matching this pattern (strict, no dots or slashes)
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,100}\\.((jpg)|(jpeg)|(png)|(gif))$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");

        // Validate and sanitize user input
        if (fileName == null || !FILENAME_PATTERN.matcher(fileName).matches()) {
            // Secure error handling: do not leak details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Resolve the canonical path to prevent directory traversal (CWE-22)
        Path imagesDir = Paths.get(IMAGES_DIR).toRealPath(LinkOption.NOFOLLOW_LINKS);
        Path requestedFile = imagesDir.resolve(fileName).normalize();

        // Ensure the requested file is inside the images directory
        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Security: Only serve existing, regular files
        if (!Files.exists(requestedFile, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(requestedFile, LinkOption.NOFOLLOW_LINKS)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Security: Set secure headers to prevent XSS, MIME sniffing, and caching of sensitive content
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "private, max-age=3600");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");

        // Security: Context-aware output encoding is handled by serving raw bytes, not user data

        // Security: Principle of least privilege - only read, never write or delete

        // Determine and set the content type securely
        String contentType = null;
        try {
            contentType = Files.probeContentType(requestedFile);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to determine content type", e);
        }
        if (contentType == null) {
            // Default to a safe content type
            contentType = "application/octet-stream";
        }
        resp.setContentType(contentType);

        // Securely stream the file
        try (InputStream in = Files.newInputStream(requestedFile, LinkOption.NOFOLLOW_LINKS);
            OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192]; // Secure default buffer size
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException ex) {
            // Secure error handling: log securely, do not leak details to user
            LOGGER.log(Level.SEVERE, "Error serving file", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve the file.");
        }
    }
}
