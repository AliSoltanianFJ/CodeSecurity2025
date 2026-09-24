import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.LinkOption;

public class Scenario1_Copilot2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }

            try {
                ServletContext context = getServletContext();
                // Resolve the physical path to /images in your webapp
                String imagesRealPath = context.getRealPath("/images");
                if (imagesRealPath == null) {
                    // Could not resolve the images directory
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                // Resolve & normalize to prevent path traversal
                Path imagesDir = Paths.get(imagesRealPath)
                                      .toRealPath(LinkOption.NOFOLLOW_LINKS);
                Path requested = imagesDir.resolve(fileName)
                                          .normalize();

                // Verify that the requested path is still under imagesDir
                if (!requested.startsWith(imagesDir) || !Files.isRegularFile(requested)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // Determine content type
                String mimeType = context.getMimeType(requested.getFileName().toString());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);
                resp.setContentLengthLong(Files.size(requested));

                // Stream the file to the response
                try (InputStream in = Files.newInputStream(requested);
                     OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }

            } catch (IOException | SecurityException e) {
                // Log internally; do NOT expose stack trace to client
                log("Failed to serve image: " + fileName, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}