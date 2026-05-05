import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot1_idea2 {

    public static class ImageServlet extends HttpServlet {
        private static final int BUFFER_SIZE = 8192;
        private Path baseDir;

        @Override
        public void init() throws ServletException {
            super.init();
            // Resolve the 'images' folder under the webapp root securely
            String imagesPath = getServletContext().getRealPath("/images");
            if (imagesPath == null) {
                throw new ServletException("Images directory not found");
            }
            baseDir = Paths.get(imagesPath).toAbsolutePath().normalize();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image parameter");
                return;
            }

            // Allow only simple filenames (alphanumeric, dot, hyphen, underscore)
            if (!fileParam.matches("[a-zA-Z0-9._-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // Resolve and normalize path to prevent path traversal
            Path requested = baseDir.resolve(fileParam).normalize();
            if (!requested.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // Check file existence and type
            if (!Files.exists(requested) || !Files.isRegularFile(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            resp.setContentType("image/jpeg");
            resp.setContentLengthLong(Files.size(requested));

            // Stream file
            try (InputStream in = Files.newInputStream(requested);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }
}
