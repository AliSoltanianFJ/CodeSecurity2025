import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot6 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Locate and normalize the images directory
            Path imagesDir = Paths.get("images").toAbsolutePath().normalize();
            Path requestedFile = imagesDir.resolve(fileName).normalize();

            // Prevent path traversal
            if (!requestedFile.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
                return;
            }

            // Check existence and that it's not a directory
            if (!Files.exists(requestedFile) || Files.isDirectory(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Determine MIME type
            ServletContext ctx = getServletContext();
            String mimeType = ctx.getMimeType(requestedFile.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requestedFile));

            // Stream the file
            try (InputStream in = Files.newInputStream(requestedFile);
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
