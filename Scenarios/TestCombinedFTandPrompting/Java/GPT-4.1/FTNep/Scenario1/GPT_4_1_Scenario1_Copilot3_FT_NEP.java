import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.io.OutputStream;
import java.io.InputStream;

public class GPT_4_1_Scenario1_Copilot3_FT_NEP extends HttpServlet {
    // Only allow file names that consist of alphanumerics, underscores, hyphens, and must end with .jpg or .png
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_-]+\\.(jpg|png)$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");

        // Validate file name strictly
        if (fileName == null || !SAFE_FILENAME.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        Path imagesDir = Paths.get(System.getenv("IMAGE_FOLDER") != null ? System.getenv("IMAGE_FOLDER") : "/var/www/images").toAbsolutePath().normalize();
        Path requestedFile;

        try {
            requestedFile = imagesDir.resolve(fileName).normalize();
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
            return;
        }

        // Ensure the requested file is within the images directory
        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check if the file exists and is a regular file
        if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Set a safe content type (do not trust user input)
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(requestedFile);
        } catch (IOException e) {
            // Fallback to default
        }
        resp.setContentType(contentType != null ? contentType : "application/octet-stream");

        // Stream the file securely
        try (InputStream in = Files.newInputStream(requestedFile);
            OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
