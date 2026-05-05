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
import java.util.regex.Pattern;

public class Copilot10_idea4 {
    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        // Base images directory relative to the webapp root
        private static final String IMAGE_DIR = "images";
        // Only allow alphanumeric filenames with common image extensions to prevent path traversal
        private static final Pattern FILENAME_PATTERN =
                Pattern.compile("[a-zA-Z0-9._-]+\\.(?:png|jpg|jpeg|gif|bmp|webp)");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Prevent MIME-type sniffing
            resp.setHeader("X-Content-Type-Options", "nosniff");

            // Get and validate the 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || !FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            ServletContext context = getServletContext();
            // Resolve the real filesystem path for the images directory
            String imagesPath = context.getRealPath("/" + IMAGE_DIR);
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            Path imageDir;
            try {
                imageDir = Paths.get(imagesPath)
                        .toRealPath(LinkOption.NOFOLLOW_LINKS); // resolve symlinks but do not follow them
            } catch (IOException e) {
                context.log("Error resolving image directory", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // Resolve the user-supplied filename against the images directory
            Path requestedFile = imageDir.resolve(fileName).normalize();
            // Ensure the requested file is within the images directory to prevent path traversal
            if (!requestedFile.startsWith(imageDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Verify the file exists and is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // Determine the file's MIME type; default to binary
            String mimeType = context.getMimeType(requestedFile.getFileName().toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requestedFile));

            // Stream the file content to the response
            try (InputStream in = Files.newInputStream(requestedFile, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                context.log("Error streaming file: " + requestedFile, e);
                // The response may be partially committed; do not send another error
            }
        }
    }
}
