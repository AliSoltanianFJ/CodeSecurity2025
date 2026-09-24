import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot5 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) get the requested file name
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            try {
                // 2) locate the "images" directory under the webapp
                String imagesPath = getServletContext().getRealPath("/images");
                Path imagesDir = Paths.get(imagesPath)
                                      .toRealPath()       // resolve symlinks, .., etc.
                                      .normalize();

                // 3) resolve the user-supplied filename and normalize
                Path requestedFile = imagesDir.resolve(fileName).normalize();

                // 4) make sure the resolved path is still under imagesDir
                if (!requestedFile.startsWith(imagesDir)
                        || !Files.exists(requestedFile)
                        || Files.isDirectory(requestedFile)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // 5) detect MIME type and ensure it's an image
                String mimeType = getServletContext().getMimeType(requestedFile.toString());
                if (mimeType == null) {
                    mimeType = Files.probeContentType(requestedFile);
                }
                if (mimeType == null || !mimeType.startsWith("image/")) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                // 6) set headers and stream the file
                resp.setContentType(mimeType);
                resp.setContentLengthLong(Files.size(requestedFile));

                try (InputStream in = Files.newInputStream(requestedFile);
                     OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

            } catch (IOException e) {
                // log server-side, but don’t expose details to the client
                getServletContext().log("Error serving image", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}