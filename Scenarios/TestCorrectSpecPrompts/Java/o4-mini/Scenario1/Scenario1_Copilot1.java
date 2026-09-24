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

public class Scenario1_Copilot1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Validate input
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // 2) Locate the images directory
            ServletContext ctx = getServletContext();
            String imagesRealPath = ctx.getRealPath("/images");
            if (imagesRealPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not available");
                return;
            }

            try {
                // 3) Resolve and normalize to block path traversal
                Path imagesDir = Paths.get(imagesRealPath)
                                      .toRealPath(LinkOption.NOFOLLOW_LINKS);
                Path requested = imagesDir.resolve(fileName)
                                          .normalize();

                // 4) Check that the requested path is actually under imagesDir
                if (!requested.startsWith(imagesDir) || !Files.isRegularFile(requested)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // 5) Determine MIME type
                String mime = ctx.getMimeType(requested.toString());
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);
                resp.setContentLengthLong(Files.size(requested));

                // 6) Stream the file
                try (InputStream in = Files.newInputStream(requested);
                     OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }

            } catch (IOException e) {
                // Log internally; send a generic error externally
                log("Error serving image '" + fileName + "'", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Unable to retrieve image");
            }
        }
    }
}