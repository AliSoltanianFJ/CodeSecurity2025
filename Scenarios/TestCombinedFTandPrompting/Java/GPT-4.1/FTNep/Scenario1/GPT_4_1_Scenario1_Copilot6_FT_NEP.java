import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;

public class GPT_4_1_Scenario1_Copilot6_FT_NEP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario1_Copilot6_FT_NEP.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || !isValidFileName(fileName)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        ServletContext context = getServletContext();
        String imagesDir = context.getRealPath("/images");
        Path imagesDirPath;
        try {
            imagesDirPath = Paths.get(imagesDir).toRealPath();
        } catch (IOException | InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "Cannot resolve images directory", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
            return;
        }

        Path imagePath;
        try {
            imagePath = imagesDirPath.resolve(fileName).normalize();
        } catch (InvalidPathException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
            return;
        }

        if (!imagePath.startsWith(imagesDirPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        String mimeType = context.getMimeType(fileName);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        resp.setContentType(mimeType);
        resp.setHeader("X-Content-Type-Options", "nosniff");

        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(imagePath, out);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error serving file", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file.");
        }
    }

    private boolean isValidFileName(String fileName) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") || fileName.startsWith(".")) {
            return false;
        }
        if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
            return false;
        }
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        boolean validExtension = false;
        for (String ext : allowedExtensions) {
            if (fileName.toLowerCase().endsWith(ext)) {
                validExtension = true;
                break;
            }
        }
        if (!validExtension) {
            return false;
        }
        return true;
    }
}
