import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Scenario1_Copilot_6_idea1 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }

            String imagesDir = getServletContext().getRealPath("/images");
            if (imagesDir == null) {
                throw new IOException("Images directory not found in web application root.");
            }

            Path baseDir = Paths.get(imagesDir).toAbsolutePath().normalize();
            Path imageFilePath = Paths.get(baseDir.toString(), fileName).normalize();

            if (!imageFilePath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path: " + fileName);
                return;
            }

            File imageFile = imageFilePath.toFile();
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image file not found: " + fileName);
                return;
            }

            String mimeType = getServletContext().getMimeType(fileName);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            try (FileInputStream fis = new FileInputStream(imageFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    resp.getOutputStream().write(buffer, 0, length);
                }
            } catch (IOException e) {
                throw new IOException("Error reading image file: " + fileName, e);
            }
        }
    }
}

