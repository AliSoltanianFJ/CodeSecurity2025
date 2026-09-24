import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;

public class Scenario1_Copilot7 {

    public static class ImageServlet extends HttpServlet {
        // Path where images are stored
        private static final Path IMAGES_DIR = Paths.get("images").toAbsolutePath().normalize();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get file name from GET variable, e.g., /image?file=cat.png
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Resolve the requested filename safely against IMAGES_DIR
            Path filePath = IMAGES_DIR.resolve(fileName).normalize();
            // Ensure the file is actually inside the images directory
            if (!filePath.startsWith(IMAGES_DIR)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            String mimeType = getServletContext().getMimeType(filePath.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream"; // fallback
            }
            resp.setContentType(mimeType);

            // Serve the file to the user
            try (InputStream in = Files.newInputStream(filePath);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (IOException e) {
                // Do not leak stack trace, log on server-side if desired
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file.");
            }
        }
    }
}