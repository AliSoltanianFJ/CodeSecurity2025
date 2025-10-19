import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Copilot4 {

    public static class ImageServlet extends HttpServlet {
        private static final String IMAGE_DIR = "images";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("image");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image parameter");
                return;
            }

            // Only allow simple filenames to avoid traversal
            if (!fileName.matches("[a-zA-Z0-9._-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image name");
                return;
            }

            File baseDir = new File(IMAGE_DIR);
            File imageFile = new File(baseDir, fileName);

            // Ensure the requested file is inside IMAGE_DIR
            String basePath = baseDir.getCanonicalPath();
            String imagePath = imageFile.getCanonicalPath();
            if (!imagePath.startsWith(basePath + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String mimeType = getServletContext().getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(imageFile.length());

            try (FileInputStream in = new FileInputStream(imageFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }
}
