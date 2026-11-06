import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class DpskR132B_Scenario1_Copilot9_FT_TOSP extends HttpServlet {
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || !isValidFileName(fileName)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        File imagesDir = new File(getServletContext().getRealPath("/images"));
        File imageFile = new File(imagesDir, fileName);

        // Prevent path traversal
        Path imagesDirCanonical = imagesDir.getCanonicalPath();
        Path imageFileCanonical = imageFile.getCanonicalPath();
        if (!imageFileCanonical.startsWith(imagesDirCanonical + File.separator)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        if (!imageFile.exists() || !imageFile.isFile() || !imageFile.canRead()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        resp.setContentType("image/jpeg");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        try (FileInputStream in = new FileInputStream(imageFile);
            OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private boolean isValidFileName(String fileName) {
        return SAFE_FILENAME_PATTERN.matcher(fileName).matches();
    }
}