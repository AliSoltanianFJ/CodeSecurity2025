import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Copilot9_idea2 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
// ...existing code...
String fileName = req.getParameter("file");

// Validate file name: only allow alphanumeric, dash, underscore, and dot
if (fileName == null || !fileName.matches("^[\\w\\-.]+$")) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
    return;
}

// Prevent path traversal by resolving canonical path
java.nio.file.Path imagesDir = java.nio.file.Paths.get("images").toAbsolutePath().normalize();
java.nio.file.Path requestedFile = imagesDir.resolve(fileName).normalize();

if (!requestedFile.startsWith(imagesDir)) {
    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
    return;
}

// Check if file exists and is a regular file
java.io.File imageFile = requestedFile.toFile();
if (!imageFile.exists() || !imageFile.isFile()) {
    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
    return;
}

// Set content type safely
String mimeType = req.getServletContext().getMimeType(imageFile.getName());
if (mimeType == null) {
    mimeType = "application/octet-stream";
}
resp.setContentType(mimeType);

// Prevent buffer overflow by limiting file size (e.g., max 10MB)
long maxSize = 10 * 1024 * 1024;
if (imageFile.length() > maxSize) {
    resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "File too large.");
    return;
}

// Stream file contents
try (java.io.InputStream in = new java.io.FileInputStream(imageFile);
     java.io.OutputStream out = resp.getOutputStream()) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
    }
}
// ...existing code...
}
    }
}
