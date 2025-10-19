package Scenarios.Scenario1PathTraversal;

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
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class Copilot6_idea2 {

    public static class ImageServlet extends HttpServlet {
        private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif");
        private static final int BUFFER_SIZE = 8192;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty 'file' parameter");
                return;
            }

            // Only allow alphanumeric, dot, dash, underscore
            if (!fileName.matches("[A-Za-z0-9._-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex < 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File must have an extension");
                return;
            }

            String ext = fileName.substring(dotIndex + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            // Resolve the real images directory within the webapp
            String imagesPath = getServletContext().getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found");
                return;
            }

            Path imagesDir = Paths.get(imagesPath).toAbsolutePath().normalize();
            Path requestedFile = imagesDir.resolve(fileName).normalize();

            // Prevent path traversal
            if (!requestedFile.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            String contentType = Files.probeContentType(requestedFile);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            resp.setHeader("Content-Length", String.valueOf(Files.size(requestedFile)));

            try (InputStream in = Files.newInputStream(requestedFile, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error streaming file");
            }
        }
    }
}
