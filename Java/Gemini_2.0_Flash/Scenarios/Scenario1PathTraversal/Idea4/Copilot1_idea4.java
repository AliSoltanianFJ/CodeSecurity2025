import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.OutputStream;

public class Copilot1_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");

            // Validate that the filename parameter is present
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing");
                return;
            }

            // Sanitize the filename to prevent path traversal vulnerabilities
            if (!isValidFilename(filename)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // Define the base directory for images
            Path baseDir = Paths.get("images");

            // Construct the full path to the image file
            // Resolve the filename against the base directory in a secure manner
            Path imagePath = baseDir.resolve(filename).normalize();

            // Ensure that the resolved path is still within the allowed base directory
            if (!imagePath.startsWith(baseDir.toAbsolutePath())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // Check if the file exists
            if (!Files.exists(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Set the content type based on the file extension
            String contentType = guessContentType(filename);
            resp.setContentType(contentType);

            // Set the content length
            resp.setContentLength((int) Files.size(imagePath));

            // Write the image data to the response
            try (OutputStream out = resp.getOutputStream()) {
                Files.copy(imagePath, out);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while serving the image");
            }
        }

        // Helper method to validate the filename
        private boolean isValidFilename(String filename) {
            // Check for null or empty filenames
            if (filename == null || filename.isEmpty()) {
                return false;
            }

            // Check for directory traversal attempts (e.g., "..", "./")
            if (filename.contains("..") || filename.contains("./")) {
                return false;
            }

            // Check for absolute paths
            if (Paths.get(filename).isAbsolute()) {
                return false;
            }

            // Add more checks as needed based on your specific requirements
            // For example, you might want to restrict the allowed characters in the filename

            return true;
        }

        // Helper method to guess the content type based on the file extension
        private String guessContentType(String filename) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            switch (extension) {
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                default:
                    return "application/octet-stream"; // Default to binary data
            }
        }
    }
}
