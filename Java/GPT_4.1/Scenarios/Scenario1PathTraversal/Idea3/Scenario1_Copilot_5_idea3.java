import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_5_idea3 extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
            return;
        }
        // Only allow safe file names (no path traversal, no special chars)
        if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
            return;
        }
        // Resolve canonical path and ensure it's within the images dir
        File imagesDir = new File("/var/www/images");
        File imageFile = new File(imagesDir, fileName);
        String imagesDirCanonical = imagesDir.getCanonicalPath();
        String imageFileCanonical = imageFile.getCanonicalPath();
        if (!imageFileCanonical.startsWith(imagesDirCanonical + File.separator)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        // Only serve files that exist and are files
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }
        // Set secure headers (no caching, no content sniffing)
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        // Content type based on file extension
        String contentType = "application/octet-stream";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (fileName.endsWith(".png")) contentType = "image/png";
        else if (fileName.endsWith(".gif")) contentType = "image/gif";
        resp.setContentType(contentType);
        resp.setStatus(HttpServletResponse.SC_OK);
        try (var out = resp.getOutputStream(); var in = Files.newInputStream(imageFile.toPath())) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }
}