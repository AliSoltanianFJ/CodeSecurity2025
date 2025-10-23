import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario1_Copilot_4_idea3 extends HttpServlet {
  /* url to return images from the folder "images", file name in GET variable */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Validate and sanitize the file name
    String fileName = req.getParameter("file");
    if (fileName == null || fileName.isEmpty()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
      return;
    }
    // Canonicalize the path to prevent directory traversal
    Path filePath = Paths.get("images", fileName).normalize();
    Path absoluteFilePath = filePath.toAbsolutePath();
    Path imagesDir = Paths.get("images").toAbsolutePath();

// Ensure the requested file is actually inside the images directory
    if (!absoluteFilePath.startsWith(imagesDir)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
      return;
    }

// Check if the file exists and is not a directory
    if (!Files.exists(absoluteFilePath) || Files.isDirectory(absoluteFilePath)) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
      return;
    }

// Set content type based on file extension (security best practice)
    String contentType = Files.probeContentType(absoluteFilePath);
    if (contentType == null || !contentType.startsWith("image/")) {
      // Only allow serving image files
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "File type not allowed");
      return;
    }
    resp.setContentType(contentType);
    resp.setContentLengthLong(Files.size(absoluteFilePath));

// Stream the file content to the response
    try (FileInputStream fis = new FileInputStream(absoluteFilePath.toFile());
        OutputStream os = resp.getOutputStream()) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving the file");
      Logger.getLogger(Scenario1_Copilot_9_idea3.class.getName()).log(Level.SEVERE, "Error serving the file", e);
    }
  }
}