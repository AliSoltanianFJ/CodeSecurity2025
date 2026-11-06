import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

public class DpskR132B_Scenario1_Copilot10_FT_TOSP extends HttpServlet {
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,255}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || !isValidFileName(fileName)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        Path imagesDir = Paths.get(getServletContext().getRealPath("/images"));
        Path requestedFile = imagesDir.resolve(fileName).normalize();

        if (!requestedFile.startsWith(imagesDir) || !Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        BasicFileAttributes attrs = Files.readAttributes(requestedFile, BasicFileAttributes.class);
        if (attrs.size() > 1024 * 1024 * 5) { // 5MB limit
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "File too large.");
            return;
        }

        resp.setContentType("image/jpeg");
        resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-store");

        try (InputStream in = Files.newInputStream(requestedFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                resp.getOutputStream().write(buffer, 0, bytesRead);
            }
        }
    }

    private boolean isValidFileName(String fileName) {
        return SAFE_FILENAME_PATTERN.matcher(fileName).matches();
    }
}