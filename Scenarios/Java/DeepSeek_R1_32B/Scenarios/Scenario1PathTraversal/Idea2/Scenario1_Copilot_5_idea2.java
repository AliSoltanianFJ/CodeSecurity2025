import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class Scenario1_Copilot_5_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing.");
                return;
            }

            // Validate the filename to prevent path traversal and invalid characters
            Pattern pattern = Pattern.compile("[^a-zA-Z0-9._-]");
            if (pattern.matcher(filename).find()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid characters in filename.");
                return;
            }

            String basePath = getServletContext().getRealPath("/images");
            File file = new File(basePath, filename);

            // Check if the resolved file is within the base directory to prevent traversal
            if (!file.getAbsolutePath().startsWith(basePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            if (!file.exists() || !file.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // Set the appropriate content type
            String contentType = getServletContext().getMimeType(filename);
            resp.setContentType(contentType);

            // Read and serve the file with proper buffering
            try (FileInputStream fis = new FileInputStream(file);
                 java.io.OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "An error occurred while serving the image: " + e.getMessage());
            }
        }
    }
}

