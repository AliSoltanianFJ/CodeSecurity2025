import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Scenario1_Copilot_1_idea1 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required.");
                return;
            }

            // Get the real path of the images directory
            String imagesDirPath = getServletContext().getRealPath("/images");
            if (imagesDirPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
                return;
            }

            // Construct the full image file path using Path API to handle relative paths safely
            Path imagePath = Paths.get(imagesDirPath, fileName).normalize();
            Path baseDir = Paths.get(imagesDirPath).toAbsolutePath().normalize();

            // Check if the resolved path is within the base directory
            if (!imagePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            java.io.File imageFile = imagePath.toFile();
            
            // Check if the file exists and is a regular file
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Set the content type based on the file's MIME type
            String mimeType = getServletContext().getMimeType(fileName);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // Stream the file to the response
            try (FileInputStream fis = new FileInputStream(imageFile);
                 java.io.OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Handle any possible IOExceptions
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image: " + e.getMessage());
            }
        }
    }
}

