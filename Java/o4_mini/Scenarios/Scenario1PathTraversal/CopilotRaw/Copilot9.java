import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Copilot9 {

    public static class ImageServlet extends HttpServlet {
        private static final String IMAGE_FOLDER = "images";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty file parameter");
                return;
            }

            // Prevent path traversal by disallowing ".." or separator characters
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            ServletContext context = getServletContext();
            String imagesDir = context.getRealPath(IMAGE_FOLDER);
            File baseDir = new File(imagesDir);
            File imageFile = new File(baseDir, fileName);

            // Ensure the resolved file is still under the images directory
            String basePath = baseDir.getCanonicalPath();
            String imagePath = imageFile.getCanonicalPath();
            if (!imagePath.startsWith(basePath + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            String mimeType = context.getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(imageFile.length());

            try (InputStream in = new FileInputStream(imageFile);
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
