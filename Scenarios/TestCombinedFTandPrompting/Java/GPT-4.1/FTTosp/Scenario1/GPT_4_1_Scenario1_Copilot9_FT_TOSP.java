import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario1_Copilot9_FT_TOSP extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario1_Copilot9_FT_TOSP.class.getName());
    private static final String IMAGE_DIR = "images";
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final int BUFFER_SIZE = 8192;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || !FILENAME_PATTERN.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        Path imagePath = Paths.get(IMAGE_DIR, fileName).normalize();
        Path canonicalImageDir = Paths.get(IMAGE_DIR).toAbsolutePath().normalize();
        Path canonicalImagePath = imagePath.toAbsolutePath().normalize();

        if (!canonicalImagePath.startsWith(canonicalImageDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        if (!Files.exists(canonicalImagePath) || !Files.isRegularFile(canonicalImagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        String mimeType = Files.probeContentType(canonicalImagePath);
        if (mimeType == null || (!mimeType.startsWith("image/"))) {
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Invalid image type.");
            return;
        }

        resp.setContentType(mimeType);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Security-Policy",
                "default-src 'none'; img-src 'self'; script-src 'none'; style-src 'none';");
        resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

        try (OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            try (var in = Files.newInputStream(canonicalImagePath)) {
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error serving image", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }
}
