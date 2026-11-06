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

public class O4_Mini_Scenario1_Copilot5_FT_TOSP {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Retrieve and validate the 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }
            // Allow only letters, digits, hyphen, underscore, and one of the approved extensions
            if (!fileName.matches("(?i)^[a-z0-9_-]+\\.(png|jpe?g|gif)$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 2) Resolve the real path to the /images folder in the webapp
            ServletContext context = getServletContext();
            String imagesDir = context.getRealPath("/images");
            if (imagesDir == null) {
                // Could not locate the images directory
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image storage unavailable.");
                return;
            }

            // 3) Canonicalize to prevent path-traversal attacks
            Path basePath;
            try {
                basePath = Paths.get(imagesDir).toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            Path requestedPath = basePath.resolve(fileName).normalize();
            if (!requestedPath.startsWith(basePath)) {
                // Attempt to escape the images directory
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 4) Verify the file exists and is a regular file
            if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 5) Determine MIME type and set headers
            String mimeType = context.getMimeType(requestedPath.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            try {
                resp.setContentLengthLong(Files.size(requestedPath));
            } catch (IOException e) {
                // If we can't determine size, proceed without Content-Length
            }

            // 6) Stream the file to the client using a fixed-size buffer
            try (InputStream in = Files.newInputStream(requestedPath, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                // Don't reveal internal details to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}

