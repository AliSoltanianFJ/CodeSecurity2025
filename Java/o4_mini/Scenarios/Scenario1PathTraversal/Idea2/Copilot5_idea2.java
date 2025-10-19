import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Copilot5_idea2 {

    public static class ImageServlet extends HttpServlet {
        // only allow simple filenames ending in a common image extension
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_\\-]+\\.(?i)(jpg|jpeg|png|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            ServletContext context = getServletContext();
            String imagesPath = context.getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Images directory not available");
                return;
            }

            Path imageDir = Paths.get(imagesPath).toAbsolutePath().normalize();
            Path filePath = imageDir.resolve(filename).normalize();
            // block any path traversal
            if (!filePath.startsWith(imageDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            String contentType = context.getMimeType(filePath.toString());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            resp.setContentLengthLong(Files.size(filePath));

            // stream the file in safe buffer sizes
            try (InputStream in = Files.newInputStream(filePath);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
