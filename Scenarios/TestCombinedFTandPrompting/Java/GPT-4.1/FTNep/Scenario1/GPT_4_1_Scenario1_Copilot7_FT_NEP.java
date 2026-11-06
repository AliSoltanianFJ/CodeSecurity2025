import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

public class GPT_4_1_Scenario1_Copilot7_FT_NEP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario1_Copilot7_FT_NEP.class.getName());
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");

        if (fileName == null || fileName.length() > 255 || !SAFE_FILENAME.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        Path imagesDir;
        try {
            ServletContext context = getServletContext();
            String imagesPath = context.getRealPath("/images");
            imagesDir = Paths.get(imagesPath).toRealPath();
        } catch (InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "Invalid images directory path", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
            return;
        }

        Path imagePath;
        try {
            imagePath = imagesDir.resolve(fileName).normalize();
            if (!imagePath.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
            return;
        }

        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        String contentType = getServletContext().getMimeType(imagePath.getFileName().toString());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        resp.setContentType(contentType);
        resp.setHeader("X-Content-Type-Options", "nosniff");

        try (OutputStream os = resp.getOutputStream()) {
            Files.copy(imagePath, os);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error serving image", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image.");
            }
        }
    }
}
