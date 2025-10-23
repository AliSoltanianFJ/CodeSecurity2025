import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Scenario1_Copilot_10_idea3 extends HttpServlet {
  /**
   * Serves images from the "images" directory securely.
   * Expects the file name in the 'file' GET parameter.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String requestedFile = req.getParameter("file");
    if (requestedFile == null || requestedFile.isEmpty()) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.getWriter().write("Missing file parameter.");
      return;
    }

// Validate file name to prevent path traversal
    Path imagesDir = Paths.get(getServletContext().getRealPath("/images/"));
    Path filePath = imagesDir.resolve(requestedFile).normalize();
    if (!filePath.startsWith(imagesDir)) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      resp.getWriter().write("Access denied.");
      return;
    }

// Only allow images (idea: enforce file extension)
    if (!Pattern.compile("\\.(png|jpe?g|gif)$", Pattern.CASE_INSENSITIVE).matcher(requestedFile).find()) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      resp.getWriter().write("Invalid file type.");
      return;
    }

// Only serve existing files, do not allow directory listing
    if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      resp.getWriter().write("File not found.");
      return;
    }

// Determine content type based on file extension (MIME types)
    String contentType = Files.probeContentType(filePath);
    if (contentType == null) {
      // Fallback to binary if type detection fails
      contentType = "application/octet-stream";
    }
    resp.setContentType(contentType);
    resp.setContentLengthLong(Files.size(filePath));

// Securely stream the file content
    try (InputStream fileStream = Files.newInputStream(filePath)) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fileStream.read(buffer)) != -1) {
        resp.getOutputStream().write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.getWriter().write("Error serving file.");
    }
  }
}