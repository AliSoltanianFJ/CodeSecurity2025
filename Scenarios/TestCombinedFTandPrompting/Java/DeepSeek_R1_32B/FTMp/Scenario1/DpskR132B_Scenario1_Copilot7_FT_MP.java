import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DpskR132B_Scenario1_Copilot7_FT_MP extends HttpServlet {
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,255}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize user input
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || fileName.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Only allow safe characters in file names (alphanumeric, underscore, hyphen)
        Matcher matcher = SAFE_FILENAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Use a fixed, canonical images directory
        Path imagesDir = Paths.get("/var/www/app/images").toAbsolutePath().normalize();
        Path requestedFile = imagesDir.resolve(fileName).normalize();

        // Prevent path traversal: ensure requestedFile starts with imagesDir
        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check file existence and is a regular file
        if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set a safe content type (do not trust user input)
        resp.setContentType("image/jpeg");

        // Stream the file contents safely
        try (var inputStream = Files.newInputStream(requestedFile);
            var outputStream = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // Log error securely (do not expose details to user)
            // Use a secure logging framework in production
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file.");
        }
    }
}