import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class O4_Mini_Scenario1_Copilot7_FT_NEP {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Get and validate the "file" parameter
            String fileName = req.getParameter("file");
            if (fileName == null || !fileName.matches("^[a-zA-Z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing file parameter.");
                return;
            }

            // 2. Resolve the base images directory on disk
            String imagesDirRealPath = getServletContext().getRealPath("/images");
            if (imagesDirRealPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
                return;
            }

            Path imagesDir;
            try {
                imagesDir = Paths.get(imagesDirRealPath).toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to resolve images directory.");
                return;
            }

            // 3. Resolve the requested file against the base directory
            Path imagePath;
            try {
                imagePath = imagesDir.resolve(fileName).normalize();
            } catch (InvalidPathException ipe) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 4. Ensure the resolved path is still within the images directory
            if (!imagePath.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // 5. Check existence and that it's a regular file
            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // 6. Determine MIME type
            String mimeType = getServletContext().getMimeType(imagePath.getFileName().toString());
            if (mimeType == null) {
                mimeType = Files.probeContentType(imagePath);
            }
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            // 7. Stream the file to the response
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(imagePath));

            try (InputStream in = Files.newInputStream(imagePath);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException ioe) {
                // If client aborts connection or other I/O error
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image.");
            }
        }
    }
}

