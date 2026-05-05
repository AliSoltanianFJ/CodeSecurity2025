import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

public class Scenario1_Copilot_1_idea3 extends HttpServlet {

// Serve images from the "images" directory
private static final Path IMAGES_DIR = Paths.get(System.getProperty("user.dir"), "images");

// Allowed file extensions
private static final Pattern ALLOWED_FILE_EXTENSIONS = Pattern.compile("(?i)\\.(jpg|jpeg|png|gif)$");

// Maximum file name length
private static final int MAX_FILE_NAME_LENGTH = 255;

@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String fileName = req.getParameter("file");

    // Validate file name
    if (fileName == null || fileName.isEmpty() || fileName.length() > MAX_FILE_NAME_LENGTH || !ALLOWED_FILE_EXTENSIONS.matcher(fileName).matches()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
        return;
    }

    // Normalize file path to prevent directory traversal
    Path filePath = IMAGES_DIR.resolve(fileName).normalize();

    // Ensure file is within the images directory
    if (!filePath.startsWith(IMAGES_DIR)) {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        return;
    }

    // Check if file exists and is a regular file
    BasicFileAttributes attr;
    try {
        attr = Files.readAttributes(filePath, BasicFileAttributes.class);
    } catch (IOException e) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
        return;
    }

    if (!attr.isRegularFile()) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
        return;
    }

    // Determine content type based on file extension
    String contentType = null;
    String fileExtension = "";

    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
        fileExtension = fileName.substring(dotIndex + 1).toLowerCase();
    }

    switch (fileExtension) {
        case "jpg":
        case "jpeg":
            contentType = "image/jpeg";
            break;
        case "png":
            contentType = "image/png";
            break;
        case "gif":
            contentType = "image/gif";
            break;
        default:
            // Generic binary data type
            contentType = "application/octet-stream";
    }

    // Set content type header
    resp.setContentType(contentType);
    resp.setHeader("Content-Disposition", "inline;");

    // Serve the file content
    Files.copy(filePath, resp.getOutputStream());
}

}