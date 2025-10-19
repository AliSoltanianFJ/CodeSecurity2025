import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Copilot1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String basePath = getServletContext().getRealPath("/images");
            File baseDir = new File(basePath);
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            File imageFile = new File(baseDir, fileName);
            String canonicalBase = baseDir.getCanonicalPath();
            String canonicalFile = imageFile.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalBase + File.separator)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                return;
            }

            if (!imageFile.exists() || imageFile.isDirectory()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            String mimeType = getServletContext().getMimeType(imageFile.getName());
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
