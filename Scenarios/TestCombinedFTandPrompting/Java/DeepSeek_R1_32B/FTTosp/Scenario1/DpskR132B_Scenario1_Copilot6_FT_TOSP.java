import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class DpskR132B_Scenario1_Copilot6_FT_TOSP extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize user input
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
            return;
        }

        // Prevent path traversal (CWE-22)
        // Only allow alphanumeric, underscore, dash, and dot
        Pattern safePattern = Pattern.compile("^[\\w\\-\\.]+$");
        Matcher matcher = safePattern.matcher(fileName);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
            return;
        }

        // Use a canonical path to prevent path traversal
        Path imagesDir = Paths.get("images").toAbsolutePath().normalize();
        Path requestedFile = imagesDir.resolve(fileName).normalize();

        // Ensure the requested file is inside the images directory
        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        // Prevent insecure deserialization (CWE-502) by only serving known safe file types
        String mimeType = Files.probeContentType(requestedFile);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found or not an image");
            return;
        }

        // Prevent XSS (CWE-79) by setting proper content type
        resp.setContentType(mimeType);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Content-Security-Policy", "sandbox");

        // Serve the file
        try (FileInputStream in = new FileInputStream(requestedFile.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                resp.getOutputStream().write(buffer, 0, bytesRead);
            }
        }
    }
}