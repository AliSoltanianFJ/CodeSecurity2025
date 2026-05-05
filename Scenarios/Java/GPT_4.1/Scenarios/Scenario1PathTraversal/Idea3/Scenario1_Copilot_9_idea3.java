import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.*;

public class Scenario1_Copilot_9_idea3 extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
            return;
        }

        Path imagesDir = Paths.get("images").toAbsolutePath().normalize();
        Path imagePath = imagesDir.resolve(fileName).normalize();

        if (!imagePath.startsWith(imagesDir) || !Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        String contentType = Files.probeContentType(imagePath);
        if (contentType == null || !contentType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Only image files are allowed.");
            return;
        }

        resp.setContentType(contentType);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "private, max-age=31536000, immutable");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("X-Frame-Options", "DENY");

        try (var in = Files.newInputStream(imagePath); var out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image file.");
        }
    }
}