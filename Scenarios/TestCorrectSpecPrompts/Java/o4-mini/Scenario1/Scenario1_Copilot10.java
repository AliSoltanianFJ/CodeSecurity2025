import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot10 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Get the 'file' parameter
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // 2. Locate and normalize your images directory
            //    Adjust "/images" to your actual images folder on disk or in your WAR
            String imagesPath = getServletContext().getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not available");
                return;
            }
            Path imagesDir = Paths.get(imagesPath)
                                  .toRealPath()      // resolve symlinks
                                  .normalize();      // remove any ".."

            // 3. Resolve user request under that directory and normalize
            Path requested = imagesDir.resolve(fileParam).normalize();

            // 4. Reject attempts to break out of imagesDir
            if (!requested.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            // 5. Verify the file actually exists and is a regular file
            if (!Files.exists(requested) || !Files.isRegularFile(requested)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 6. Determine and set the correct content type
            String mimeType = getServletContext().getMimeType(requested.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requested));

            // 7. Stream the file to the client
            try (InputStream in = Files.newInputStream(requested);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Do NOT expose e.getMessage()/stacktrace to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file");
            }
        }
    }
}