import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot1 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("file");
            
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File parameter is missing");
                return;
            }

            Path imagePath = Paths.get(filename).normalize();
            String imagesDir = getServletContext().getRealPath("/images");
            
            if (imagesDir == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found");
                return;
            }

            Path baseDir = Paths.get(imagesDir).normalize();
            Path resolvedPath = baseDir.resolve(imagePath).normalize();
            if (!resolvedPath.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
                return;
            }

            if (!Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image file not found");
                return;
            }

            String contentType = getServletContext().getMimeType(resolvedPath.toString());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            resp.setContentType(contentType);
            resp.setContentLengthLong(Files.size(resolvedPath));
            resp.setDateHeader("Last-Modified", Files.getLastModifiedTime(resolvedPath).toMillis());

            java.io.OutputStream out = resp.getOutputStream();
            Files.copy(resolvedPath, out);
            out.close();
        }
    }
}
