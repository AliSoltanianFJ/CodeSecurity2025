import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class Scenario1_Copilot7 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Get the requested file name
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image file parameter.");
                return;
            }

            // 2) Determine the real file‐system path to the /images directory
            //    (Assuming your images folder lives under webapp root: /images)
            String imagesPath = getServletContext().getRealPath("/images");
            if (imagesPath == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Images directory is unavailable.");
                return;
            }

            try {
                // 3) Resolve and normalize both the base dir and the user‐supplied name
                Path imagesDir = Paths.get(imagesPath).toRealPath().normalize();
                Path requestedFile = imagesDir.resolve(fileName).normalize();

                // 4) Ensure the requested file is still under our images directory
                if (!requestedFile.startsWith(imagesDir)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid image path.");
                    return;
                }

                // 5) Check existence and that it’s a regular file
                if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // 6) Lookup a MIME type and set headers
                String mime = getServletContext().getMimeType(requestedFile.toString());
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);
                resp.setContentLengthLong(Files.size(requestedFile));

                // 7) Stream the file out
                try (InputStream in = Files.newInputStream(requestedFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        resp.getOutputStream().write(buffer, 0, len);
                    }
                }
            } catch (IOException e) {
                // Log full stack on server, but give client only a generic message
                log("Error while serving image: " + fileName, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Unable to retrieve image.");
            }
        }
    }
}