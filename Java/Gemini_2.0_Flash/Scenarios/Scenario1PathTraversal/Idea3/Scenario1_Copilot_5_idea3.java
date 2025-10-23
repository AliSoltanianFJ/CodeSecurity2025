import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**

    A secure servlet for serving images from a predefined directory.

    Avoids directory traversal and only allows specific file types.

    All user input is validated and sanitized.
    */
public class Scenario1_Copilot_5_idea3 extends HttpServlet {

    // Security: Principle of least privilege
    // Do not use system properties (user.dir) for file paths
    // All paths should be absolute and canonicalized
    private static final String IMAGE_DIR = "/opt/images";
    // Allowed image file extensions
    private static final Pattern ALLOWED_EXTENSIONS = Pattern.compile("^(?i)(png|jpe?g|gif)$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Validate and sanitize user input
    String filename = req.getParameter("file");
    if (filename == null || filename.isEmpty()) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
    return;
    }

     // Ensure filename does not contain directory traversal characters
     Path safeFilename = Paths.get(filename).normalize();
     if (!safeFilename.resolve(Paths.get(IMAGE_DIR)).toAbsolutePath().startsWith(IMAGE_DIR)) {
         resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
         return;
     }

     // Check file extension against the allowlist
     String filenameStr = safeFilename.getFileName().toString();
     if (filenameStr.contains("..") || !ALLOWED_EXTENSIONS.matcher(filenameStr).matches()) {
         resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file type or path");
         return;
     }

     // Construct the absolute file path
     Path filePath = Paths.get(IMAGE_DIR).resolve(safeFilename).normalize().toAbsolutePath();

     // Security: Principle of least privilege
     // Do not allow serving files outside the designated directory
     if (!filePath.startsWith(IMAGE_DIR)) {
         resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
         return;
     }

     // Security: Principle of least privilege
     // Only allow reading existing files, no write/execute permissions
     if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
         resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
         return;
     }

     // Set the appropriate content type based on file extension
     String contentType = guessContentType(filenameStr);
     if (contentType == null) {
         // Fallback: generic binary data
         contentType = "application/octet-stream";
     }
     resp.setContentType(contentType);

     // Security: Prevent Clickjacking by setting X-Frame-Options
     resp.setHeader("X-Frame-Options", "DENY");

     // Security: Mitigate MIME-sniffing attacks
     resp.setHeader("X-Content-Type-Options", "nosniff");

     // Security: Secure HTTPS only (in production)
     resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

     // Set cache headers (production: use appropriate caching strategy)
     resp.setHeader("Cache-Control", "public, max-age=31536000");

     // Try to read file metadata to set last modified
     try {
         BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
         FileTime lastModifiedTime = attrs.lastModifiedTime();
         Instant instant = lastModifiedTime.toInstant();
         ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss z", Locale.US);
         String lastModified = formatter.format(zdt);
         resp.setHeader("Last-Modified", lastModified);
     } catch (IOException e) {
         // Log error, but do not leak sensitive details to the client
     }

     // Stream the file content to the response
     try {
         Files.copy(filePath, resp.getOutputStream());
     } catch (IOException e) {
         resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving the file");
     }

    }

    /**
        Simple content type guessing based on file extension.
        In production, use a proper MIME type mapping library.
        */
        private String guessContentType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
        case "png": return "image/png";
        case "jpeg": case "jpg": return "image/jpeg";
        case "gif": return "image/gif";
        default: return null;
        }
        }
        }