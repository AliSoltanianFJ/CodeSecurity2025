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

public class Scenario1_Copilot6 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) get the filename parameter
            String filename = req.getParameter("file");
            if (filename == null || filename.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty 'file' parameter");
                return;
            }

            // 2) locate the "images" directory in the webapp
            ServletContext ctx = getServletContext();
            String imagesDirReal = ctx.getRealPath("/images");
            if (imagesDirReal == null) {
                // container may not expand, or directory missing
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not available");
                return;
            }

            // 3) resolve and normalize the requested file
            Path imagesDir = Paths.get(imagesDirReal)
                                  .toRealPath(LinkOption.NOFOLLOW_LINKS); 
            Path requested = imagesDir.resolve(filename).normalize();

            // 4) reject traversal outside of imagesDir
            if (!requested.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // 5) check existence and that it's a regular file
            if (!Files.exists(requested) || !Files.isRegularFile(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // 6) determine content type
            String contentType = ctx.getMimeType(requested.toString());
            if (contentType == null) {
                contentType = Files.probeContentType(requested);
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            resp.setContentLengthLong(Files.size(requested));

            // 7) stream the file out
            try (InputStream in = Files.newInputStream(requested);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
            } catch (IOException ioe) {
                // log internally but don't expose stack trace to client
                log("Error serving image '" + filename + "'", ioe);
                if (!resp.isCommitted()) {
                    resp.reset();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve file");
                }
            }
        }
    }
}