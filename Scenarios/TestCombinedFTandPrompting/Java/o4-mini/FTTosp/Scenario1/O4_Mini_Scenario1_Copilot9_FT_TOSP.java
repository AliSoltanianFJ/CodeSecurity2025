import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class O4_Mini_Scenario1_Copilot9_FT_TOSP {

    public static class ImageServlet extends HttpServlet {

        // Only allow filenames matching this pattern:
        //   - letters, digits, dot, underscore, hyphen
        //   - ends with a safe image extension
        private static final String FILENAME_REGEX =
            "^[a-zA-Z0-9._-]+\\.(?i)(jpg|jpeg|png|gif|bmp)$";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Get and validate the "file" parameter
            String filename = req.getParameter("file");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image file parameter.");
                return;
            }
            if (!filename.matches(FILENAME_REGEX)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 2) Locate the "images" directory within the webapp
            ServletContext context = getServletContext();
            String imagesPath = context.getRealPath("/images");
            if (imagesPath == null) {
                // Could happen if the container doesn't unpack to a file system
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Images directory unavailable.");
                return;
            }

            // 3) Resolve and normalize to prevent directory traversal
            Path baseDir;
            try {
                baseDir = Paths.get(imagesPath)
                               .toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException ioe) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Cannot access images directory.");
                return;
            }

            Path imageFile = baseDir.resolve(filename).normalize();
            if (!imageFile.startsWith(baseDir)) {
                // Attempt to break out of /images
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // 4) Check existence, readability, and regular file
            if (!Files.exists(imageFile) ||
                !Files.isRegularFile(imageFile) ||
                !Files.isReadable(imageFile))
            {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // 5) Determine and set Content‐Type
            String mimeType = context.getMimeType(imageFile.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // 6) Optionally set Content‐Length
            try {
                long size = Files.size(imageFile);
                if (size >= 0 && size <= Integer.MAX_VALUE) {
                    resp.setContentLength((int) size);
                } else if (size > Integer.MAX_VALUE) {
                    resp.setHeader("Content-Length", Long.toString(size));
                }
            } catch (IOException ignored) {
                // Best‐effort; if it fails, the container will chunk‐encode
            }

            // 7) Stream the file in a fixed‐size buffer
            try (InputStream in = Files.newInputStream(imageFile, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream())
            {
                byte[] buffer = new byte[8 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException ioe) {
                // Log but do not expose internal details to the client
                context.log("Error streaming image: " + filename, ioe);
            }
        }
    }
}

