import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class DpskR132B_Scenario1_Copilot4_FT_MP extends HttpServlet {
    // Allow only safe characters (alphanumeric, underscore, dash, dot) and limit path depth
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,255}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate that the 'file' parameter exists and matches the safe pattern
        String fileName = req.getParameter("file");
        if (fileName == null || !SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Resolve the real path to the images directory
        String imagesDir = getServletContext().getRealPath("/images");
        if (imagesDir == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
            return;
        }

        // Combine with the sanitized file name to get the full path
        Path imagePath = Paths.get(imagesDir, fileName).toAbsolutePath().normalize();

        // Check that the file exists and is inside the images directory
        File imageFile = imagePath.toFile();
        if (!imageFile.exists() || !imagePath.startsWith(Paths.get(imagesDir).toAbsolutePath().normalize())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Set secure headers
        resp.setContentType("image/jpeg"); // or use a library to detect MIME type
        resp.setCharacterEncoding("UTF-8");

        // Stream the image safely
        try (var inputStream = Files.newInputStream(imagePath);
            var outputStream = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}