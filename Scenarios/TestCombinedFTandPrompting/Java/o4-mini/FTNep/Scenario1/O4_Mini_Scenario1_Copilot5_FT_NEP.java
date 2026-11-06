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
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot5_FT_NEP {

    public static class ImageServlet extends HttpServlet {
        // allow only filenames with letters, digits, dot, underscore or hyphen
        private static final Pattern FILENAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty() || !FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing file parameter.");
                return;
            }

            // Resolve the absolute path to the /images directory
            ServletContext ctx = getServletContext();
            String imagesRealPath = ctx.getRealPath("/images");
            if (imagesRealPath == null) {
                // no exploded directory on disk
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not available.");
                return;
            }

            Path imagesDir;
            try {
                imagesDir = Paths.get(imagesRealPath).toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot access image directory.");
                return;
            }

            // Resolve requested file and prevent path‐traversal
            Path requestedFile = imagesDir.resolve(fileName).normalize();
            if (!requestedFile.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }

            // Check file existence and that it is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile, LinkOption.NOFOLLOW_LINKS)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Determine MIME type
            String mimeType = ctx.getMimeType(requestedFile.toString());
            if (mimeType == null) {
                mimeType = Files.probeContentType(requestedFile);
            }
            if (mimeType == null || !mimeType.startsWith("image/")) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Not an image.");
                return;
            }

            // Stream file contents
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requestedFile));

            try (InputStream in = Files.newInputStream(requestedFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }
}

