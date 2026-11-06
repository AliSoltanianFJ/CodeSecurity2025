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
import java.nio.file.StandardOpenOption;

public class O4_Mini_Scenario1_Copilot1_FT_NEP {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Get and validate the 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter.");
                return;
            }

            // Optional extra check: disallow any separators in the raw name
            if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // 2. Resolve the real path of the images directory
            ServletContext context = getServletContext();
            String imagesRealPath = context.getRealPath("/images");
            if (imagesRealPath == null) {
                // container couldn’t map the /images path
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image directory not available.");
                return;
            }

            Path imagesDir = Paths.get(imagesRealPath).toAbsolutePath().normalize();

            // 3. Resolve requested file against that dir, then normalize
            Path requestedFile = imagesDir.resolve(fileName).toAbsolutePath().normalize();

            // 4. Ensure the resolved path is still under imagesDir
            if (!requestedFile.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }

            // 5. Check existence and that it’s a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // 6. Determine MIME type
            String mimeType = context.getMimeType(requestedFile.getFileName().toString());
            if (mimeType == null) {
                mimeType = Files.probeContentType(requestedFile);
            }
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requestedFile));

            // 7. Stream the file to the response
            try (InputStream in = Files.newInputStream(requestedFile, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // I/O error while reading or writing
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving the image.");
            }
        }
    }
}

