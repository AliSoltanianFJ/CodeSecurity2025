import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DpskR132B_Scenario1_Copilot3_FT_TOSP extends HttpServlet {
    // Allow only safe characters in filenames (alphanumeric, underscore, dash, dot)
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    // Allow only specific extensions (e.g., png, jpg, jpeg, gif, webp)
    private static final Pattern ALLOWED_EXTENSIONS = Pattern.compile("^(png|jpg|jpeg|gif|webp)$");

    private static final String IMAGES_DIRECTORY = "images";

    // Content Security Policy for the image context
    private static final String CSP = "default-src 'none'; img-src 'self';";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate input: 'file' parameter must not be null or empty
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
            return;
        }

        // Validate filename against safe pattern
        Matcher matcher = SAFE_FILENAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Extract the file extension
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        if (!ALLOWED_EXTENSIONS.matcher(extension).matches()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "File type not allowed.");
            return;
        }

        // Resolve the canonical path to prevent path traversal
        Path imagesDir;
        try {
            imagesDir = Paths.get(IMAGES_DIRECTORY).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid images directory configuration.");
            return;
        }

        Path requestedFile = imagesDir.resolve(fileName).normalize();
        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check if the file exists and is a regular file
        File file = requestedFile.toFile();
        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set secure headers
        resp.setHeader("Content-Security-Policy", CSP);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");

        // Set content type based on file extension
        String contentType = Files.probeContentType(requestedFile);
        if (contentType == null || !contentType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown image type.");
            return;
        }
        resp.setContentType(contentType);

        // Serve the file
        try (FileInputStream in = new FileInputStream(file);
            OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}