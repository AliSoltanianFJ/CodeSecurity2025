import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot9_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Validate and sanitize the "file" parameter
            String filename = req.getParameter("file");
            if (filename == null || !filename.matches("[\\w\\-]+\\.(png|jpg|jpeg|gif)")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing 'file' parameter");
                return;
            }

            // 2. Resolve the base images directory securely
            ServletContext context = getServletContext();
            String imagesDir = context.getRealPath("/images");
            if (imagesDir == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not available");
                return;
            }

            Path baseDir = Paths.get(imagesDir).toRealPath().normalize();
            Path imagePath = baseDir.resolve(filename).normalize();

            // 3. Prevent path traversal & ensure file exists and is not a directory
            if (!imagePath.startsWith(baseDir) || !Files.exists(imagePath) || Files.isDirectory(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 4. Determine and set the MIME type
            String mimeType = context.getMimeType(imagePath.toString());
            if (mimeType == null) {
                mimeType = Files.probeContentType(imagePath);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(imagePath));

            // 5. Stream the image content to the response
            try (InputStream in = Files.newInputStream(imagePath);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                // Do not expose internal details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
