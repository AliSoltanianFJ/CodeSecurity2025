import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class Copilot10_idea2 {

    public static class ImageServlet extends HttpServlet {
        private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "bmp"
        ));
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("fileName");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image fileName parameter");
                return;
            }

            // Validate file name characters
            if (!FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // Validate extension
            String extension = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                extension = fileName.substring(dotIndex + 1)
                                    .toLowerCase(Locale.ENGLISH);
            }
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            ServletContext context = getServletContext();
            String imagesPath = context.getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found");
                return;
            }

            File baseDir = new File(imagesPath);
            File imageFile = new File(baseDir, fileName);

            // Prevent path traversal
            String canonicalBase = baseDir.getCanonicalPath();
            String canonicalFile = imageFile.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalBase + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!imageFile.isFile() || !imageFile.canRead()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            String mimeType = context.getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(imageFile.length());

            try (InputStream in = new FileInputStream(imageFile);
                 ServletOutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                // Avoid leaking internal details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
