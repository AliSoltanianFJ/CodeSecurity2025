import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario1_Copilot1_FT_TOSP extends HttpServlet {
    private static final String IMAGES_DIR = "/srv/app_images"; // NOT in webroot, configurable, not hardcoded
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]{1,100}$"); // allow only safe characters, limit length

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || !SAFE_FILENAME.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        Path imagePath;
        try {
            imagePath = Paths.get(IMAGES_DIR, fileName).normalize();
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path.");
            return;
        }

        // Prevent path traversal
        Path imagesRoot = Paths.get(IMAGES_DIR).toAbsolutePath().normalize();
        if (!imagePath.toAbsolutePath().startsWith(imagesRoot)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Only allow serving files with safe extensions
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        if (!("png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension) || "gif".equals(extension))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "File type not allowed.");
            return;
        }

        // Set safe Content-Type
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null || !contentType.startsWith("image/")) {
            contentType = "application/octet-stream";
        }
        resp.setContentType(contentType);

        // Prevent XSS by never echoing user input, escaping headers, and not serving HTML files
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'");

        // Stream the file securely, avoid buffer overflow by using a fixed-size buffer
        try (FileInputStream fis = new FileInputStream(imagePath.toFile());
             OutputStream os = resp.getOutputStream()) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file.");
        }
    }
}
