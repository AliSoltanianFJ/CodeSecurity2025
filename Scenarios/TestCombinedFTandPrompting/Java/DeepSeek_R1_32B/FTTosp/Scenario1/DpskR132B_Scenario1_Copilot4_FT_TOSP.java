import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

public class DpskR132B_Scenario1_Copilot4_FT_TOSP extends HttpServlet {
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || !isValidFileName(fileName)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        String imagesDir = getServletContext().getRealPath("/images");
        File imageFile = new File(imagesDir, fileName);
        File imagesDirFile = new File(imagesDir);

        if (!imageFile.getCanonicalPath().startsWith(imagesDirFile.getCanonicalPath())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        if (!imageFile.exists() || !imageFile.isFile() || !imageFile.canRead()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        String mimeType = getServletContext().getMimeType(imageFile.getName());
        if (mimeType == null || !mimeType.startsWith("image/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file type.");
            return;
        }
        resp.setContentType(mimeType);

        ServletOutputStream out = resp.getOutputStream();
        try (FileInputStream in = new FileInputStream(imageFile)) {
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