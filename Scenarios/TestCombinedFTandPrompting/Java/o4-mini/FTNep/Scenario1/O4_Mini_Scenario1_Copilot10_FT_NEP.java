import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class O4_Mini_Scenario1_Copilot10_FT_NEP {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Get and validate 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image file name.");
                return;
            }
            // only allow simple filenames: letters, digits, underscore, dash, dot and space
            if (!fileName.matches("[\\w\\-. ]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file name.");
                return;
            }

            // 2. Locate the images directory on disk
            ServletContext ctx = getServletContext();
            String imagesRealPath = ctx.getRealPath("/images");
            if (imagesRealPath == null) {
                // container cannot resolve real path
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory unavailable.");
                return;
            }

            // 3. Resolve and normalize the requested file path
            Path imagesDir = Paths.get(imagesRealPath)
                                  .toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path requestedFile = imagesDir.resolve(fileName).normalize();

            // 4. Security check: ensure requestedFile is under imagesDir
            if (!requestedFile.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // 5. Check that it exists and is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // 6. Determine content type
            String contentType = ctx.getMimeType(requestedFile.toString());
            if (contentType == null) {
                contentType = Files.probeContentType(requestedFile);
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);

            // 7. Optionally set content length
            long size = Files.size(requestedFile);
            if (size <= Integer.MAX_VALUE) {
                resp.setContentLength((int) size);
            } else {
                resp.setContentLengthLong(size);
            }

            // 8. Stream the file
            try (InputStream in = Files.newInputStream(requestedFile, StandardOpenOption.READ);
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

