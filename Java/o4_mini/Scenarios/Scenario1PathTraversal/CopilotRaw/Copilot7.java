import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot7 {

    public static class ImageServlet extends HttpServlet {
        // adjust this to point at your "images" directory
        private static final Path IMAGES_DIR = Paths.get("images").toAbsolutePath().normalize();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Resolve and normalize to prevent path traversal
            Path requestedPath = IMAGES_DIR.resolve(fileParam).normalize();
            if (!requestedPath.startsWith(IMAGES_DIR)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            File requestedFile = requestedPath.toFile();
            if (!requestedFile.exists() || !requestedFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Determine content type
            String mimeType = req.getServletContext().getMimeType(requestedFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(requestedFile.length());

            // Stream the file
            try (InputStream in = new BufferedInputStream(new FileInputStream(requestedFile));
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
