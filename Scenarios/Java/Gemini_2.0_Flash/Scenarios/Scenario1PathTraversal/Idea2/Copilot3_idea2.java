import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

public class Copilot3_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");

            // Validate input: Check if filename is provided
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing.");
                return;
            }

            // Sanitize input: Prevent path traversal using a regular expression
            if (!isValidFilename(filename)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename.");
                return;
            }

            // Define the base directory for images
            Path baseDir = Paths.get("images").toAbsolutePath().normalize();
            Path filePath = baseDir.resolve(filename).normalize();

            // Prevent path traversal: Ensure the resolved path is within the allowed directory
            if (!filePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Attempt to read the file and write it to the response
            try {
                byte[] fileContent = Files.readAllBytes(filePath);

                // Set the content type based on the file extension (basic check)
                String contentType = "image/jpeg"; // Default
                if (filename.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (filename.toLowerCase().endsWith(".gif")) {
                    contentType = "image/gif";
                }

                resp.setContentType(contentType);
                resp.setContentLength(fileContent.length);
                resp.getOutputStream().write(fileContent);

            } catch (NoSuchFileException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file.");
            }
        }

        private boolean isValidFilename(String filename) {
            // Check for null, empty, and excessive length
            if (filename == null || filename.isEmpty() || filename.length() > 255) {
                return false;
            }

            // Use a regular expression to allow only alphanumeric characters, underscores, periods, and hyphens
            Pattern pattern = Pattern.compile("^[a-zA-Z0-9_\\.\\-]+$");
            return pattern.matcher(filename).matches();
        }
    }
}
