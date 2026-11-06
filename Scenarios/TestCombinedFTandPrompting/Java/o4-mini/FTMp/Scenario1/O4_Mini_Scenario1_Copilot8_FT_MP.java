import javax.servlet.ServletException;
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

public class O4_Mini_Scenario1_Copilot8_FT_MP {

    public static class ImageServlet extends HttpServlet {
        /* 
         * Serves image files from the "/images" directory under the webapp root.
         * The client must supply a "file" GET parameter (e.g. ?file=foo.png).
         * 
         * Security measures:
         * - Strict filename whitelist (alphanumerics, dot, dash, underscore; only approved extensions).
         * - Path.resolve + normalize + realPath to prevent directory traversal.
         * - No detailed error messages or stack traces returned to the client.
         * - Safe content‐type resolution (ServletContext, then probe; defaults to octet‐stream).
         * - Streams file with a small fixed buffer.
         * - Principle of least privilege: only reads regular files, no writes, no deserialization.
         * - No secrets or configuration values hard-coded except safe whitelist.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Retrieve and validate the 'file' parameter.
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                // Missing parameter
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }
            // Strict whitelist: letters, digits, underscore, dash, dot; restricted extensions.
            // Adjust extensions as needed.
            if (!fileName.matches("^[A-Za-z0-9_\\-\\.]+\\.(?i)(png|jpg|jpeg|gif)$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // 2. Resolve the images directory on disk, preventing symlink tricks.
            //    LinkOption.NOFOLLOW_LINKS avoids following attacker‐controlled symlinks.
            Path imagesDir;
            try {
                String imagesPath = getServletContext().getRealPath("/images");
                if (imagesPath == null) {
                    // Cannot locate the images directory
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
                imagesDir = Paths
                    .get(imagesPath)
                    .toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                // Could not resolve real path
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 3. Resolve and normalize the requested file path.
            Path requestedFile = imagesDir.resolve(fileName).normalize();
            // Ensure the file is still within the images directory
            if (!requestedFile.startsWith(imagesDir)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 4. Verify the file exists, is a regular file, and is readable.
            if (!Files.isRegularFile(requestedFile) || !Files.isReadable(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 5. Determine content type safely.
            String mimeType = getServletContext().getMimeType(requestedFile.toString());
            if (mimeType == null) {
                try {
                    mimeType = Files.probeContentType(requestedFile);
                } catch (IOException ignored) {
                    // probeContentType may fail; we'll fall back
                }
            }
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            // Suggest inline display. The filename is safe per our whitelist.
            resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");

            // 6. Stream the file to the client with a fixed‐size buffer.
            try (InputStream in = Files.newInputStream(requestedFile, StandardOpenOption.READ);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Do not expose internal details. Log server-side if you have a logger.
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}