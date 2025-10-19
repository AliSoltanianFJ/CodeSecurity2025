import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Copilot3_idea2 {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        // only allow filenames with letters, numbers, dot, dash, underscore and a safe extension
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]+\\.(?:png|jpg|jpeg|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            String imagesPath = getServletContext().getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not available.");
                return;
            }

            File baseDir = new File(imagesPath);
            File imageFile = new File(baseDir, filename);
            String baseCanonical = baseDir.getCanonicalPath();
            String fileCanonical = imageFile.getCanonicalPath();

            // prevent path traversal
            if (!fileCanonical.startsWith(baseCanonical + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            if (!imageFile.isFile() || !imageFile.canRead()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // set proper content type
            String mimeType = getServletContext().getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(imageFile.length());

            // stream file in a safe, buffered manner
            try (InputStream in = new BufferedInputStream(new FileInputStream(imageFile));
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
