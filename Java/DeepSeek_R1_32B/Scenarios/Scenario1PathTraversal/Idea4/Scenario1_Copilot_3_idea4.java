import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_3_idea4 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("filename");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }

            // Validate the filename to prevent directory traversal and invalid characters
            if (!isValidFileName(fileName)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename format.");
                return;
            }

            String basePath = req.getServletContext().getRealPath("/images/");
            Path baseDir = Paths.get(basePath);
            Path requestedFile = baseDir.resolve(fileName);

            // Normalize the path to prevent directory traversal
            Path normalizedPath = requestedFile.normalize();

            if (!normalizedPath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Check if the file exists and is a regular file
            if (!Files.exists(normalizedPath) || !Files.isRegularFile(normalizedPath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Set the content type based on the file's MIME type
            String mimeType = req.getServletContext().getMimeType(normalizedPath.toString());
            if (mimeType != null) {
                resp.setContentType(mimeType);
            } else {
                // Default to a binary stream if MIME type cannot be determined
                resp.setContentType("application/octet-stream");
            }

            try {
                byte[] imageBytes = Files.readAllBytes(normalizedPath);
                resp.getOutputStream().write(imageBytes);
                resp.getOutputStream().flush();
            } catch (IOException e) {
                // Log the error and send a 500 error response
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to read the image file.");
                throw e;
            }
        }

        private boolean isValidFileName(String fileName) {
            // Allow letters, numbers, underscores, periods, and hyphens only
            return fileName.matches("^[a-zA-Z0-9_\\.-]+$");
        }
    }
}

