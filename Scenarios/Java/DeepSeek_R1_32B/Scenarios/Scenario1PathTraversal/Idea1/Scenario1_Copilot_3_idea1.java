import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Scenario1_Copilot_3_idea1 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }

            // Get the real path of the images directory
            String baseDir = getServletContext().getRealPath("/images");
            if (baseDir == null) {
                throw new IOException("Images folder not found in web application root");
            }

            // Sanitize and resolve the file path
            Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
            Path filePath = Paths.get(basePath.toString(), fileName)
                                 .toAbsolutePath()
                                 .normalize();

            if (!filePath.startsWith(basePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            java.io.File imageFile = new java.io.File(filePath.toString());
            
            // Check if the file exists and is a regular file
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found: " + fileName);
                return;
            }

            // Set content type based on file extension
            String mimeType = getServletContext().getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // Stream the file to the response
            try (java.io.FileInputStream fis = new java.io.FileInputStream(imageFile);
                 java.io.OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading the image.");
            }
        }
    }
}

