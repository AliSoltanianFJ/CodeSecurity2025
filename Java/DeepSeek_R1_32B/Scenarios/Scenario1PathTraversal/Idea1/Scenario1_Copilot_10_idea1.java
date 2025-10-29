import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletContext;

public class Scenario1_Copilot_10_idea1 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is required.");
                return;
            }

            // Sanitize the filename to prevent directory traversal
            String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "");

            ServletContext sc = getServletContext();
            String imagesDirPath = sc.getRealPath("/images");
            if (imagesDirPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
                return;
            }

            Path imagesDir = Paths.get(imagesDirPath).toAbsolutePath().normalize();
            Path imagePath = imagesDir.resolve(sanitizedFileName).normalize();

            // Ensure the resolved path is within the images directory
            if (!imagePath.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            java.io.File imageFile = imagePath.toFile();
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            String mimeType = sc.getMimeType(imageFile.getName());
            resp.setContentType(mimeType);

            try (InputStream is = new FileInputStream(imageFile);
                 OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Log the error
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image.");
            }
        }
    }
}

