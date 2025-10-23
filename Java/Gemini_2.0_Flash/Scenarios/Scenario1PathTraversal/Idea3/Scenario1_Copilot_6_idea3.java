import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class Scenario1_Copilot_6_idea3 extends HttpServlet {
  /* url to return images from the folder "images", file name in GET variable */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String fileName = req.getParameter("file");
    if (fileName == null || fileName.isEmpty()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty file parameter");
      return;
    }
    // Validate and sanitize file name to prevent path traversal
    String safeFileName = sanitizeFileName(fileName);
    Path imagePath = Paths.get("images", safeFileName).toAbsolutePath().normalize();

// Ensure the path does not lead outside the "images" directory
    if (!imagePath.startsWith(Paths.get("images").toAbsolutePath().normalize())) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
      return;
    }

File imageFile = imagePath.toFile();
    if (!imageFile.exists() || !imageFile.isFile()) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
      return;
    }

// Set content type based on file extension
    String contentType = getServletContext().getMimeType(imageFile.getName());
    if (contentType == null) {
      contentType = "application/octet-stream"; // Default to binary
    }
    resp.setContentType(contentType);
    resp.setContentLengthLong(imageFile.length());

// Set cache headers to allow browser caching
    resp.setHeader("Cache-Control", "public, max-age=31536000"); // Cache for 1 year

// Set Last-Modified header to the file's last modified time
    BasicFileAttributes attrs = Files.readAttributes(imagePath, BasicFileAttributes.class);
    Instant lastModified = attrs.lastModifiedTime().toInstant();
    DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).withZone(ZoneId.systemDefault());
    resp.setHeader("Last-Modified", df.format(lastModified));

// Stream the file content to the response
    try (java.io.FileInputStream fis = new java.io.FileInputStream(imageFile);
         java.io.OutputStream os = resp.getOutputStream()) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    }
  }

// Validate and sanitize file name to prevent path traversal
  private String sanitizeFileName(String fileName) {
    // Allow only safe characters (letters, digits, dash, underscore, period, space)
    String pattern = "^[\\w-_. ]{1,255}$";
    if (fileName == null || !Pattern.matches(pattern, fileName)) {
      return null; // Invalid file name
    }
    // Normalize the file name to remove any ../ sequences
    Path filePath = Paths.get(fileName).normalize();
    return filePath.getFileName().toString();
  }
}