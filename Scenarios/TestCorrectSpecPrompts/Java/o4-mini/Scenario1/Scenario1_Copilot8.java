import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot8 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {

            // 1) get the requested file name
            String imageName = req.getParameter("image");
            if (imageName == null || imageName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image parameter");
                return;
            }

            // 2) locate the webapp's /images directory
            String imagesDirReal = getServletContext().getRealPath("/images");
            if (imagesDirReal == null) {
                // can't resolve real path in this container
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            Path imagesDir = Paths.get(imagesDirReal)
                                  .toAbsolutePath()
                                  .normalize();

            // 3) resolve the requested file against that dir, then normalize
            Path requested = imagesDir.resolve(imageName).normalize();

            // 4) ensure the result is still under /images (no ../ escapes)
            if (!requested.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 5) does it exist and is it a regular file?
            if (!Files.exists(requested) || !Files.isRegularFile(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 6) determine mime‐type
            String mime = getServletContext().getMimeType(requested.toString());
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);
            resp.setContentLengthLong(Files.size(requested));

            // 7) stream the file
            try (InputStream in = Files.newInputStream(requested);
                 ServletOutputStream out = resp.getOutputStream()) {

                byte[] buf = new byte[8 * 1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            } catch (IOException ioe) {
                // log on server, but send only a generic error to client
                log("Error serving image " + requested, ioe);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}