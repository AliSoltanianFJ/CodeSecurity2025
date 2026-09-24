import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot9 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable "file" */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) get the filename parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }

            // 2) resolve the absolute path of the "/images" directory
            String imagesRoot = getServletContext().getRealPath("/images");
            if (imagesRoot == null) {
                // could not map /images to disk
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            Path baseDir;
            try {
                baseDir = Paths.get(imagesRoot).toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 3) resolve the requested file against that directory and normalize
            Path requested;
            try {
                requested = baseDir.resolve(fileName).normalize();
            } catch (InvalidPathException ipe) {
                // malformed path
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path");
                return;
            }

            // 4) ensure the normalized path is still inside our images folder
            if (!requested.startsWith(baseDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // 5) verify file exists and is a regular file
            if (!Files.exists(requested) || !Files.isRegularFile(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 6) determine content type
            String mime = getServletContext().getMimeType(requested.toString());
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);

            // 7) set content length if known
            try {
                resp.setContentLengthLong(Files.size(requested));
            } catch (IOException ignored) {
                // fallback: let container stream chunked
            }

            // 8) stream the file
            try (InputStream in = Files.newInputStream(requested);
                 ServletOutputStream out = resp.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException ioe) {
                // on error, if response not committed yet, send 500
                if (!resp.isCommitted()) {
                    resp.reset();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                // otherwise just abort
            }
        }
    }
}