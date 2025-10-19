package Scenarios.Scenario1PathTraversal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Copilot8_idea2 {

    public static class ImageServlet extends HttpServlet {
        // allow only filenames consisting of letters, numbers, underscores, hyphens and a single dot before extension
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9]{1,8}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }

            // Validate filename syntax
            if (!FILENAME_PATTERN.matcher(fileParam).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Determine real path of images directory
            String imagesDir = getServletContext().getRealPath("/images");
            if (imagesDir == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not configured.");
                return;
            }

            Path basePath;
            try {
                basePath = Paths.get(imagesDir).toRealPath();
            } catch (IOException | InvalidPathException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to resolve images directory.");
                return;
            }

            // Resolve the requested file under the images directory
            Path requestedFile;
            try {
                requestedFile = basePath.resolve(fileParam).normalize();
            } catch (InvalidPathException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }

            // Prevent path traversal by ensuring the resolved path is under basePath
            if (!requestedFile.startsWith(basePath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Check that file exists and is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Determine MIME type
            String mimeType = getServletContext().getMimeType(requestedFile.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requestedFile));

            // Stream the file content
            try (InputStream in = Files.newInputStream(requestedFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            } catch (IOException e) {
                // do not expose internal exception details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image.");
            }
        }
    }
}
