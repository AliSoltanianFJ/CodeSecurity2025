import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_3_idea3 extends HttpServlet {
  /* url to return images from the folder "images", file name in GET variable */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Get the file name from the request
    String fileName = req.getParameter("file");
    if (fileName == null || fileName.isEmpty()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty file parameter");
      return;
    }

// Validate file name to prevent directory traversal
    if (fileName.contains("..") || fileName.contains("\\") || fileName.contains("/")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
      return;
    }

// Security: never trust user input; always validate and sanitize

// Define the base directory for images
    // Assuming "images" folder is in the webapp's root
    String baseDir = getServletContext().getRealPath("/images");
    if (baseDir == null) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found");
      return;
    }
    Path basePath = Paths.get(baseDir);

// Construct the file path
    Path filePath = basePath.resolve(fileName).normalize();

// Ensure the requested file is within the allowed directory
    if (!filePath.startsWith(basePath)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
      return;
    }

// Check if the file exists
    if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
      return;
    }

// Determine content type (basic check)
    String contentType = Files.probeContentType(filePath);
    if (contentType == null || !contentType.startsWith("image/")) {
      resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported file type");
      return;
    }

// Set content type
    resp.setContentType(contentType);

// Set cache control headers
    resp.setHeader("Cache-Control", "public, max-age=86400"); // 1 day

// Copy file content to response output stream
    Files.copy(filePath, resp.getOutputStream());
  }
}