import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot3 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Get requested file name
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }

            // 2) Locate the images directory
            String imagesRealPath = getServletContext().getRealPath("/images");
            if (imagesRealPath == null) {
                // container might not expand WARs to the file system
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory unavailable.");
                return;
            }
            Path imagesDir = Paths.get(imagesRealPath).normalize();

            // 3) Resolve the requested file against imagesDir and normalize
            final Path requested;
            try {
                requested = imagesDir.resolve(fileName).normalize();
            } catch (InvalidPathException ipe) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }

            // 4) Ensure requested path is under imagesDir and is a file
            if (!requested.startsWith(imagesDir) || !Files.exists(requested) || !Files.isRegularFile(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 5) Determine MIME type
            String mime = getServletContext().getMimeType(requested.getFileName().toString());
            if (mime == null) {
                try {
                    mime = Files.probeContentType(requested);
                } catch (IOException e) {
                    // ignore and fall through
                }
            }
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);

            // 6) Optionally set content length
            try {
                long size = Files.size(requested);
                if (size <= Integer.MAX_VALUE) {
                    resp.setContentLength((int) size);
                } else {
                    resp.setContentLengthLong(size);
                }
            } catch (IOException e) {
                // ignore size errors
            }

            // 7) Stream the file
            try (InputStream in = Files.newInputStream(requested);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException ioe) {
                // Log server-side, but don't send the trace to the client
                log("Error streaming image " + fileName, ioe);
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }
}