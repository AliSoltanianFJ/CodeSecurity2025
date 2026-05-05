import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Copilot5_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
// ...existing code...
// Security: Validate and sanitize user input to prevent path traversal (CWE-22), XSS (CWE-79), and other attacks.
// Principle: Only allow access to files within the "images" directory, never outside.
// Error handling: Do not leak sensitive info in error messages.
// Secure defaults: Only serve files with safe extensions (e.g., .png, .jpg, .jpeg, .gif).
String fileName = req.getParameter("file");
if (fileName == null || fileName.isEmpty()) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
    return;
}

// Allow only safe characters in file names (alphanumeric, underscore, dash, dot)
if (!fileName.matches("^[a-zA-Z0-9_.-]+$")) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
    return;
}

// Only allow specific extensions
String[] allowedExtensions = {".png", ".jpg", ".jpeg", ".gif"};
boolean validExtension = false;
for (String ext : allowedExtensions) {
    if (fileName.toLowerCase().endsWith(ext)) {
        validExtension = true;
        break;
    }
}
if (!validExtension) {
    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "File type not allowed.");
    return;
}

// Resolve canonical path and ensure it's inside the images directory
java.nio.file.Path imagesDir = java.nio.file.Paths.get(getServletContext().getRealPath("/images")).toAbsolutePath().normalize();
java.nio.file.Path requestedFile = imagesDir.resolve(fileName).normalize();
if (!requestedFile.startsWith(imagesDir)) {
    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
    return;
}

// Check if file exists and is a regular file
java.io.File file = requestedFile.toFile();
if (!file.exists() || !file.isFile()) {
    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
    return;
}


resp.setContentType("image/jpeg"); // Set appropriate content type based on file extension

// Principle of least privilege: Only read, never write or execute
try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
     java.io.OutputStream os = resp.getOutputStream()) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = fis.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
    }
} catch (IOException e) {
    // Log error securely (do not expose details to user)
    // Use a secure logging framework in production
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file.");
}
// ...existing code...
}
    }
}
